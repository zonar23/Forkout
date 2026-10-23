package io.github.zonar23.forkout.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightFormatTest {

    @Test
    fun formatKg_dropsTrailingZerosAndUsesComma() {
        assertEquals("80", WeightFormat.formatKg(80_000))
        assertEquals("82,5", WeightFormat.formatKg(82_500))
        assertEquals("1,25", WeightFormat.formatKg(1_250))
        assertEquals("0", WeightFormat.formatKg(0))
    }

    @Test
    fun parseKg_acceptsCommaAndDot() {
        assertEquals(82_500, WeightFormat.parseKg("82,5"))
        assertEquals(82_500, WeightFormat.parseKg(" 82.5 "))
        assertEquals(7_500, WeightFormat.parseKg("7,5"))
        assertEquals(0, WeightFormat.parseKg("0"))
    }

    @Test
    fun parseKg_roundsToWholeGrams() {
        assertEquals(1_235, WeightFormat.parseKg("1,2345"))
    }

    @Test
    fun parseKg_rejectsInvalidInput() {
        assertNull(WeightFormat.parseKg(""))
        assertNull(WeightFormat.parseKg("abc"))
        assertNull(WeightFormat.parseKg("-5"))
        assertNull(WeightFormat.parseKg("5000"))
    }

    @Test
    fun formatAndParse_roundTrip() {
        listOf(0, 1_250, 7_500, 45_000, 82_500, 120_000).forEach { grams ->
            assertEquals(grams, WeightFormat.parseKg(WeightFormat.formatKg(grams)))
        }
    }
}
