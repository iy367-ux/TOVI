package com.limity.app

/**
 * Pure interval reconciliation helpers.
 *
 * UsageStats is an imperfect recovery source: it may omit transitions, and its
 * event boundaries are not guaranteed to match accessibility observations. Keep
 * this logic isolated so callers can validate and deduplicate recovered windows
 * before inserting them into the append-only ledger.
 */
internal object UsageReconciliation {
    data class Interval(val startMillis: Long, val endMillis: Long) {
        init {
            require(startMillis >= 0L)
            require(endMillis >= startMillis)
        }

        val durationMillis: Long get() = endMillis - startMillis
    }

    /**
     * Returns portions of [candidate] not already covered by [recorded].
     * Overlapping and adjacent recorded ranges are merged first. Empty ranges
     * are ignored; output is ordered and contains no overlap with recorded data.
     */
    fun uncovered(candidate: Interval, recorded: List<Interval>): List<Interval> {
        if (candidate.durationMillis == 0L) return emptyList()

        val merged = recorded.asSequence()
            .filter { it.durationMillis > 0L }
            .map { Interval(maxOf(it.startMillis, candidate.startMillis), minOf(it.endMillis, candidate.endMillis)) }
            .filter { it.durationMillis > 0L }
            .sortedBy { it.startMillis }
            .fold(mutableListOf<Interval>()) { acc, next ->
                val last = acc.lastOrNull()
                if (last != null && next.startMillis <= last.endMillis) {
                    acc[acc.lastIndex] = Interval(last.startMillis, maxOf(last.endMillis, next.endMillis))
                } else {
                    acc += next
                }
                acc
            }

        val result = mutableListOf<Interval>()
        var cursor = candidate.startMillis
        for (range in merged) {
            if (range.startMillis > cursor) result += Interval(cursor, range.startMillis)
            cursor = maxOf(cursor, range.endMillis)
            if (cursor >= candidate.endMillis) break
        }
        if (cursor < candidate.endMillis) result += Interval(cursor, candidate.endMillis)
        return result
    }

    /** Reject implausibly large/future windows before any recovery write. */
    fun isSane(candidate: Interval, nowMillis: Long, maximumDurationMillis: Long): Boolean =
        candidate.durationMillis > 0L &&
            candidate.durationMillis <= maximumDurationMillis &&
            candidate.endMillis <= nowMillis
}
