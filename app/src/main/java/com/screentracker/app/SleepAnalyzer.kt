package com.screentracker.app

data class SleepAnalysis(
    val totalDurationMinutes: Int,
    val interruptionCount: Int,
    val completedCycles: Int,
    val possibleCycles: Int,
    val qualityScore: Int,
    val qualityLabel: String,
    val gaps: List<GapInfo>,
    val cycleBlocks: List<CycleBlock>,
    val wakeEpisodes: List<WakeEpisode>,
    val longestStretchMinutes: Int,
    val totalAwakeMinutes: Int,
    val timeToFirstSleepMinutes: Int
)

data class GapInfo(
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val severity: GapSeverity
)

enum class GapSeverity {
    FULL_CYCLE,
    PARTIAL,
    FRAGMENTED,
    RESTLESS
}

data class CycleBlock(
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val completed: Boolean,
    val severity: GapSeverity
)

data class WakeEpisode(
    val time: Long,
    val rawEventCount: Int,
    val sleepBeforeMinutes: Int
)

object SleepAnalyzer {

    private const val CYCLE_DURATION_MIN = 90
    private const val GOAL_MINUTES = 360
    private const val CLUSTER_WINDOW_MS = 5 * 60 * 1000L // 5 minutes
    private const val BRIEF_WAKE_MINUTES = 2 // assumed duration of a single screen check

    fun analyze(
        sleepStart: Long,
        sleepEnd: Long,
        screenOnTimestamps: List<Long>
    ): SleepAnalysis {
        val totalMinutes = ((sleepEnd - sleepStart) / 60_000).toInt()
        val rawInterruptions = screenOnTimestamps.filter { it in sleepStart..sleepEnd }.sorted()

        // Cluster nearby screen events into wake episodes
        val clusters = clusterInterruptions(rawInterruptions)
        val clusteredTimestamps = clusters.map { it.first() } // use first event of each cluster
        val interruptionCount = clusters.size

        // Build gaps and blocks from clustered data
        val gaps = buildGaps(sleepStart, sleepEnd, clusteredTimestamps)
        val completedCycles = gaps.count { it.severity == GapSeverity.FULL_CYCLE }
        val possibleCycles = if (totalMinutes >= CYCLE_DURATION_MIN) totalMinutes / CYCLE_DURATION_MIN else 0
        val cycleBlocks = buildCycleBlocks(sleepStart, sleepEnd, clusteredTimestamps)

        // Build wake episodes with context
        val wakeEpisodes = buildWakeEpisodes(sleepStart, clusters)

        // Analytics
        val longestStretch = gaps.maxOfOrNull { it.durationMinutes } ?: 0
        val totalAwake = calculateAwakeTime(clusters)
        val timeToFirstSleep = if (clusteredTimestamps.isNotEmpty() && clusteredTimestamps.first() - sleepStart < 30 * 60_000L) {
            // If first wake is within 30 min, time to first sleep = time until after that first cluster
            val firstCluster = clusters.first()
            val firstClusterEnd = firstCluster.last()
            ((firstClusterEnd - sleepStart) / 60_000).toInt()
        } else {
            0 // fell asleep quickly
        }

        val qualityScore = calculateQualityScore(
            totalMinutes, interruptionCount, gaps, completedCycles, possibleCycles
        )
        val qualityLabel = when {
            qualityScore >= 85 -> "Excellent"
            qualityScore >= 65 -> "Good"
            qualityScore >= 40 -> "Fair"
            else -> "Poor"
        }

        return SleepAnalysis(
            totalDurationMinutes = totalMinutes,
            interruptionCount = interruptionCount,
            completedCycles = completedCycles,
            possibleCycles = possibleCycles,
            qualityScore = qualityScore,
            qualityLabel = qualityLabel,
            gaps = gaps,
            cycleBlocks = cycleBlocks,
            wakeEpisodes = wakeEpisodes,
            longestStretchMinutes = longestStretch,
            totalAwakeMinutes = totalAwake,
            timeToFirstSleepMinutes = timeToFirstSleep
        )
    }

    /**
     * Group SCREEN_ON events within 5 minutes of each other into clusters.
     * Each cluster represents a single wake episode.
     */
    private fun clusterInterruptions(timestamps: List<Long>): List<List<Long>> {
        if (timestamps.isEmpty()) return emptyList()
        val sorted = timestamps.sorted()
        val clusters = mutableListOf(mutableListOf(sorted.first()))

        for (i in 1 until sorted.size) {
            if (sorted[i] - clusters.last().first() > CLUSTER_WINDOW_MS) {
                clusters.add(mutableListOf(sorted[i]))
            } else {
                clusters.last().add(sorted[i])
            }
        }
        return clusters
    }

    private fun buildWakeEpisodes(
        sleepStart: Long,
        clusters: List<List<Long>>
    ): List<WakeEpisode> {
        var previousPoint = sleepStart
        return clusters.map { cluster ->
            val sleepBefore = ((cluster.first() - previousPoint) / 60_000).toInt()
            previousPoint = cluster.first()
            WakeEpisode(
                time = cluster.first(),
                rawEventCount = cluster.size,
                sleepBeforeMinutes = sleepBefore
            )
        }
    }

    private fun calculateAwakeTime(clusters: List<List<Long>>): Int {
        return clusters.sumOf { cluster ->
            if (cluster.size > 1) {
                // duration of the cluster (first to last event)
                ((cluster.last() - cluster.first()) / 60_000).toInt().coerceAtLeast(BRIEF_WAKE_MINUTES)
            } else {
                BRIEF_WAKE_MINUTES
            }
        }
    }

    private fun buildGaps(start: Long, end: Long, interruptions: List<Long>): List<GapInfo> {
        if (interruptions.isEmpty()) {
            val dur = ((end - start) / 60_000).toInt()
            return listOf(GapInfo(start, end, dur, classifyGap(dur)))
        }

        val points = mutableListOf(start) + interruptions + end
        return points.zipWithNext().map { (a, b) ->
            val dur = ((b - a) / 60_000).toInt()
            GapInfo(a, b, dur, classifyGap(dur))
        }
    }

    private fun classifyGap(minutes: Int): GapSeverity = when {
        minutes >= 90 -> GapSeverity.FULL_CYCLE
        minutes >= 60 -> GapSeverity.PARTIAL
        minutes >= 30 -> GapSeverity.FRAGMENTED
        else -> GapSeverity.RESTLESS
    }

    private fun buildCycleBlocks(start: Long, end: Long, interruptions: List<Long>): List<CycleBlock> {
        val points = if (interruptions.isEmpty()) {
            listOf(start, end)
        } else {
            mutableListOf(start) + interruptions + end
        }

        return points.zipWithNext().map { (a, b) ->
            val dur = ((b - a) / 60_000).toInt()
            val severity = classifyGap(dur)
            CycleBlock(
                startTime = a,
                endTime = b,
                durationMinutes = dur,
                completed = severity == GapSeverity.FULL_CYCLE,
                severity = severity
            )
        }
    }

    private fun calculateQualityScore(
        totalMinutes: Int,
        wakeEpisodeCount: Int,
        gaps: List<GapInfo>,
        completedCycles: Int,
        possibleCycles: Int
    ): Int {
        var score = 100

        // Duration penalty: up to -30 for being under 6 hours
        if (totalMinutes < GOAL_MINUTES) {
            val deficit = GOAL_MINUTES - totalMinutes
            score -= (deficit * 0.15).toInt().coerceAtMost(30)
        }

        // Wake episode penalty: -5 each, up to -25
        score -= (wakeEpisodeCount * 5).coerceAtMost(25)

        // Gap quality penalty (from clustered gaps)
        val fragmentedCount = gaps.count { it.severity == GapSeverity.FRAGMENTED }
        val restlessCount = gaps.count { it.severity == GapSeverity.RESTLESS }
        score -= fragmentedCount * 5
        score -= restlessCount * 8

        // Cycle completion bonus: up to +10
        if (possibleCycles > 0) {
            val cycleRatio = completedCycles.toFloat() / possibleCycles
            score += (cycleRatio * 10).toInt()
        }

        return score.coerceIn(0, 100)
    }
}
