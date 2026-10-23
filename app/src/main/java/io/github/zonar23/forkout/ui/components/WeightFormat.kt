package io.github.zonar23.forkout.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Converts between stored grams and the kilogram values shown in the UI. */
object WeightFormat {

    private const val GRAMS_PER_KG = 1000
    private const val MAX_GRAMS = 1_000_000

    /** Formats grams as kilograms with up to two decimals, e.g. 82500 → "82,5". */
    fun formatKg(grams: Int, locale: Locale = Locale.GERMAN): String {
        val format = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale))
        return format.format(BigDecimal(grams).divide(BigDecimal(GRAMS_PER_KG)))
    }

    /**
     * Parses a kilogram input such as "82,5" or "82.5" into grams.
     * Returns null for empty, negative, non-numeric or implausibly large values.
     */
    fun parseKg(input: String): Int? {
        val normalized = input.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val kg = normalized.toBigDecimalOrNull() ?: return null
        if (kg.signum() < 0) return null
        val grams = kg.multiply(BigDecimal(GRAMS_PER_KG)).setScale(0, RoundingMode.HALF_UP)
        return grams.toInt().takeIf { grams <= BigDecimal(MAX_GRAMS) }
    }
}
