package com.limity.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local-only, append-only usage ledger.
 *
 * Each foreground interval is inserted as a separate row. Totals are derived from
 * those rows, so a partially completed update cannot leave app and daily counters
 * out of sync. No app content, screen text, messages or keystrokes are collected.
 */
object UsageLedger {
    private const val DB_NAME = "tovi_usage.db"
    private const val DB_VERSION = 1
    private const val TABLE = "usage_intervals"

    private fun dayKey(date: Date = Date()): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(date)

    private class LedgerDb(context: Context) :
        SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {
        init {
            setWriteAheadLoggingEnabled(true)
        }

        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    day_key TEXT NOT NULL,
                    package_name TEXT NOT NULL,
                    duration_seconds INTEGER NOT NULL CHECK(duration_seconds > 0),
                    recorded_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX index_usage_day ON $TABLE(day_key)")
            db.execSQL("CREATE INDEX index_usage_package_day ON $TABLE(package_name, day_key)")
            db.execSQL(
                "CREATE TABLE ledger_meta (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Future schema changes must preserve the append-only history.
        }
    }

    @Volatile private var helper: LedgerDb? = null
    private val lock = Any()

    private fun database(context: Context): SQLiteDatabase = synchronized(lock) {
        val dbHelper = helper ?: LedgerDb(context).also { helper = it }
        dbHelper.writableDatabase.also { migrateLegacyPreferencesIfNeeded(context, it) }
    }

    /**
     * One-time migration from the earlier SharedPreferences counters.
     * The marker and copied rows are committed in the same transaction.
     */
    private fun migrateLegacyPreferencesIfNeeded(context: Context, db: SQLiteDatabase) {
        val marker = db.rawQuery(
            "SELECT value FROM ledger_meta WHERE key = ?",
            arrayOf("legacy_preferences_migrated")
        ).use { cursor -> cursor.moveToFirst() }

        if (marker) return

        val prefs = context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            prefs.all.forEach { (key, value) ->
                if (!key.startsWith("usage_") || key.startsWith("usage_total_")) return@forEach
                val remainder = key.removePrefix("usage_")
                if (remainder.length <= 9) return@forEach
                val day = remainder.substring(0, 8)
                val pkg = remainder.substring(9)
                val seconds = (value as? Long) ?: return@forEach
                if (day.length != 8 || pkg.isBlank() || seconds <= 0L) return@forEach

                val row = ContentValues().apply {
                    put("day_key", day)
                    put("package_name", pkg)
                    put("duration_seconds", seconds)
                    put("recorded_at", now)
                }
                db.insert(TABLE, null, row)
            }

            db.execSQL(
                "INSERT OR REPLACE INTO ledger_meta(key, value) VALUES(?, ?)",
                arrayOf("legacy_preferences_migrated", "1")
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun sum(
        context: Context,
        selection: String,
        args: Array<String>
    ): Long {
        val db = database(context)
        return db.rawQuery(
            "SELECT COALESCE(SUM(duration_seconds), 0) FROM $TABLE WHERE $selection",
            args
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    fun secondsForApp(context: Context, pkg: String, day: String = dayKey()): Long =
        sum(context, "day_key = ? AND package_name = ?", arrayOf(day, pkg))

    fun totalSeconds(context: Context, day: String = dayKey()): Long =
        sum(context, "day_key = ?", arrayOf(day))

    fun addForegroundSeconds(context: Context, pkg: String, seconds: Long) {
        if (seconds <= 0L || pkg.isBlank()) return
        val db = database(context)
        val row = ContentValues().apply {
            put("day_key", dayKey())
            put("package_name", pkg)
            put("duration_seconds", seconds)
            put("recorded_at", System.currentTimeMillis())
        }
        db.insertOrThrow(TABLE, null, row)

        val prefs = context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
        val daily = prefs.getInt("daily_tokens", 120).coerceAtLeast(0)
        val consumed = (totalSeconds(context) / 60L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        prefs.edit().putInt("tokens_remaining", (daily - consumed).coerceAtLeast(0)).apply()
    }

    fun remainingTokens(context: Context): Int {
        val prefs = context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
        val daily = prefs.getInt("daily_tokens", 120).coerceAtLeast(0)
        val consumed = (totalSeconds(context) / 60L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return (daily - consumed).coerceAtLeast(0)
    }

    fun selectedAppSeconds(
        context: Context,
        packages: Set<String>,
        day: String = dayKey()
    ): Map<String, Long> = packages.associateWith { secondsForApp(context, it, day) }

    fun weeklyTotals(context: Context): List<Pair<String, Long>> {
        val result = mutableListOf<Pair<String, Long>>()
        val label = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        for (offset in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -offset)
            val date = calendar.time
            result += label.format(date) to totalSeconds(context, dayKey(date))
        }
        return result
    }

    fun resetIfNewDay(context: Context) {
        val prefs = context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
        val today = dayKey()
        if (prefs.getString("last_reset_day", "") != today) {
            val daily = prefs.getInt("daily_tokens", 120).coerceAtLeast(0)
            val consumed = (totalSeconds(context, today) / 60L)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            prefs.edit()
                .putString("last_reset_day", today)
                .putInt("tokens_remaining", (daily - consumed).coerceAtLeast(0))
                .apply()
        }
    }
}
