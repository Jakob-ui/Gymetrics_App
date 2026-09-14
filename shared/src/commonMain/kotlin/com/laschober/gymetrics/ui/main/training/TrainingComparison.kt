package com.laschober.gymetrics.ui.main.training

// Per-exercise comparison against the same exercise (matched by index in the plan) of the
// previous training done with the same template. Only weightDone/repsDone are compared - the
// backend only stores one number of each per exercise, not one per set.
enum class Trend { UP, DOWN, SAME, UNKNOWN }
enum class Overall { PROGRESS, DECLINE, SAME, MIXED, UNKNOWN }

data class ExerciseComparison(
    val weightTrend: Trend,
    val repsTrend: Trend,
    val overall: Overall,
)

private fun trendOf(current: Double?, previous: Double?): Trend = when {
    current == null || previous == null -> Trend.UNKNOWN
    current > previous -> Trend.UP
    current < previous -> Trend.DOWN
    else -> Trend.SAME
}

fun compareExercise(
    currentWeightDone: Double?,
    currentRepsDone: Int?,
    previousWeightDone: Double?,
    previousRepsDone: Int?,
): ExerciseComparison {
    val weightTrend = trendOf(currentWeightDone, previousWeightDone)
    val repsTrend = trendOf(currentRepsDone?.toDouble(), previousRepsDone?.toDouble())

    val overall = when {
        weightTrend == Trend.UNKNOWN && repsTrend == Trend.UNKNOWN -> Overall.UNKNOWN
        weightTrend == Trend.UP && repsTrend == Trend.DOWN -> Overall.MIXED
        weightTrend == Trend.DOWN && repsTrend == Trend.UP -> Overall.MIXED
        weightTrend == Trend.DOWN || repsTrend == Trend.DOWN -> Overall.DECLINE
        weightTrend == Trend.UP || repsTrend == Trend.UP -> Overall.PROGRESS
        else -> Overall.SAME
    }

    return ExerciseComparison(weightTrend, repsTrend, overall)
}
