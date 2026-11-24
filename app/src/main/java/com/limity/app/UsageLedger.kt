package com.limity.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local-only usage ledger. Stores selected-app foreground seconds by local day and package.
 * No app content, screen text, messages or keystrokes are collected.
 */
object UsageLedger {
    private fun dayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    private fun usageKey(day: String, pkg: String) = "usage_${day}_${pkg}"
    private fun totalKey(day: String) = "usage_total_${day}"

    fun secondsForApp(context: Context, pkg: String, day: String = dayKey()): Long =
        context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
            .getLong(usageKey(day, pkg), 0L)

    fun totalSeconds(context: Context, day: String = dayKey()): Long =
        context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
            .getLong(totalKey(day), 0L)

    fun addForegroundSeconds(context: Context, pkg: String, seconds: Long) {
        if (seconds <= 0L) return
        val prefs = context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
        val day = dayKey()
        val appTotal = prefs.getLong(usageKey(day, pkg), 0L) + seconds
        val total = prefs.getLong(totalKey(day), 0L) + seconds
        val daily = prefs.getInt("daily_tokens", 120)
        val consumed = (total / 60L).toInt()
        prefs.edit()
            .putLong(usageKey(day, pkg), appTotal)
            .putLong(totalKey(day), total)
            .putInt("tokens_remaining", (daily - consumed).coerceAtLeast(0))
            .apply()
    }

    fun remainingTokens(context: Context): Int {
        val prefs = context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
        val daily = prefs.getInt("daily_tokens", 120)
        return (daily - (totalSeconds(context) / 60L).toInt()).coerceAtLeast(0)
    }

    fun selectedAppSeconds(context: Context, packages: Set<String>, day: String = dayKey()): Map<String, Long> =
        packages.associateWith { secondsForApp(context, it, day) }

    fun weeklyTotals(context: Context): List<Pair<String, Long>> {
        val prefs = context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
        val result = mutableListOf<Pair<String, Long>>()
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
        val label = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        for (offset in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -offset)
            val date = calendar.time
            val key = formatter.format(date)
            result += label.format(date) to prefs.getLong(totalKey(key), 0L)
        }
        return result
    }

    fun resetIfNewDay(context: Context) {
        val prefs = context.getSharedPreferences("tovi_settings", Context.MODE_PRIVATE)
        val today = dayKey()
        if (prefs.getString("last_reset_day", "") != today) {
            prefs.edit().putString("last_reset_day", today)
                .putInt("tokens_remaining", prefs.getInt("daily_tokens", 120)).apply()
        }
    }
}
