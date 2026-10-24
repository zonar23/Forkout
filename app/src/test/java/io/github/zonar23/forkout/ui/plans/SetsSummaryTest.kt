package io.github.zonar23.forkout.ui.plans

import io.github.zonar23.forkout.data.db.entity.PlanSetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SetsSummaryTest {

    private fun set(reps: Int, grams: Int) =
        PlanSetEntity(planExerciseId = 1, position = 0, targetReps = reps, targetWeightGrams = grams, restSeconds = 90)

    @Test
    fun equalSets_showSingleValues() {
        assertEquals(SetsSummary(3, "12", "80"), List(3) { set(12, 80_000) }.summarize())
    }

    @Test
    fun differentSets_showRanges() {
        val summary = listOf(set(12, 75_000), set(10, 80_000), set(8, 82_500)).summarize()

        assertEquals(SetsSummary(3, "8–12", "75–82,5"), summary)
    }

    @Test
    fun noSets_showZero() {
        assertEquals(SetsSummary(0, "0", "0"), emptyList<PlanSetEntity>().summarize())
    }
}
