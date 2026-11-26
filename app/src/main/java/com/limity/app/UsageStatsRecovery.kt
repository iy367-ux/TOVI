package com.limity.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/**
 * Reconstructs selected-app foreground intervals after the accessibility service
 * was stopped. Requires the user to grant Usage Access in Android Settings.
 *
 * Recovery starts at the last service heartbeat (or at most 24 hours ago) and
 * clips reconstructed intervals to that boundary. This deliberately avoids
 * trying to infer usage from aggregate UsageStats totals, which could overlap
 * the append-only ledger.
 */
object UsageStatsRecovery {
    private const val MAX_RECOVERY_MILLIS = 24 * 60 * 60 * 1000L

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun recover(context: Context, packages: Set<String>, fromMillis: Long, nowMillis: Long = System.currentTimeMillis()) {
        if (packages.isEmpty() || !hasUsageAccess(context) || nowMillis <= fromMillis) return
        val start = maxOf(fromMillis, nowMillis - MAX_RECOVERY_MILLIS)
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(start, nowMillis)
        val event = UsageEvents.Event()
        val active = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg !in packages) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> active.putIfAbsent(pkg, maxOf(start, event.timeStamp))
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val began = active.remove(pkg) ?: continue
                    val ended = event.timeStamp.coerceAtMost(nowMillis)
                    if (ended > began) UsageLedger.addForegroundInterval(context, pkg, began, ended)
                }
            }
        }

        // A selected app may still be foreground when recovery runs.
        active.forEach { (pkg, began) ->
            if (nowMillis > began) UsageLedger.addForegroundInterval(context, pkg, began, nowMillis)
        }
    }
}
