package com.screentracker.app

data class SleepAnalysis(
    val totalDurationMinutes: Int,
    val interruptionCount: Int,
    val completedCycles: Int,
    val possibleCycles: Int,
    val qualityScore: Int,
    val qualityLabel: String,
    val gaps: List<GapInfo>,
    val cycleBlocks: List<CycleBlock>
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
    val completed: Boolean,        // true if this uninterrupted stretch >= 90 min
    val severity: GapSeverity      // how this stretch classifies
)

object SleepAnalyzer {

    private const val CYCLE_DURATION_MIN = 90
    private const val GOAL_MINUTES = 360 // 6 hours

    fun analyze(
        sleepStart: Long,
        sleepEnd: Long,
        screenOnTimestamps: List<Long>
    ): SleepAnalysis {
        val totalMinutes = ((sleepEnd - sleepStart) / 60_000).toInt()
        val interruptions = screenOnTimestamps.filter { it in sleepStart..sleepEnd }.sorted()
        val interruptionCount = interruptions.size

        val gaps = buildGaps(sleepStart, sleepEnd, interruptions)
        val completedCycles = gaps.count { it.severity == GapSeverity.FULL_CYCLE }
        val possibleCycles = if (totalMinutes >= CYCLE_DURATION_MIN) totalMinutes / CYCLE_DURATION_MIN else 0
        val cycleBlocks = buildCycleBlocks(sleepStart, sleepEnd, interruptions)

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
            cycleBlocks = cycleBlocks
        )
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
        // Gap-based: each block is an actual uninterrupted stretch of sleep.
        // When you wake up, your sleep cycle resets — the next stretch starts fresh.
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
        interruptionCount: Int,
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

        // Interruption penalty: escalating, up to -40
        score -= when {
            interruptionCount == 0 -> 0
            interruptionCount <= 2 -> interruptionCount * 5
            interruptionCount <= 4 -> 10 + (interruptionCount - 2) * 8
            else -> 26 + (interruptionCount - 4) * 6
        }.coerceAtMost(40)

        // Gap quality penalty
        val fragmentedCount = gaps.count { it.severity == GapSeverity.FRAGMENTED }
        val restlessCount = gaps.count { it.severity == GapSeverity.RESTLESS }
        score -= fragmentedCount * 5
        score -= restlessCount * 10

        // Cycle completion bonus: up to +10
        if (possibleCycles > 0) {
            val cycleRatio = completedCycles.toFloat() / possibleCycles
            score += (cycleRatio * 10).toInt()
        }

        return score.coerceIn(0, 100)
    }
}
