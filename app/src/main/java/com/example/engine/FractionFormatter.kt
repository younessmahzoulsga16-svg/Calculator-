package com.example.engine

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.*

enum class AngleUnit(val label: String) {
    DEG("DEG"),
    RAD("RAD"),
    GRAD("GRAD")
}

enum class NumberDisplayMode(val label: String) {
    AUTO("AUTO"),
    FRACTION("FRAC"),
    SCIENTIFIC("SCI"),
    POLAR("POLAR")
}

object NumberFormatter {

    private val symbols = DecimalFormatSymbols(Locale.US)

    fun format(
        value: Complex,
        mode: NumberDisplayMode,
        angleUnit: AngleUnit = AngleUnit.DEG
    ): String {
        val clean = value.cleanNoise()
        if (!clean.re.isFinite() || !clean.im.isFinite()) {
            throw MathEvalException("Result out of range")
        }

        if (mode == NumberDisplayMode.POLAR) {
            val r = formatRealAuto(clean.absValue)
            val angleRad = clean.argRadians
            val angleConverted = when (angleUnit) {
                AngleUnit.DEG -> Math.toDegrees(angleRad)
                AngleUnit.RAD -> angleRad
                AngleUnit.GRAD -> angleRad * 200.0 / PI
            }
            val unitSuffix = when (angleUnit) {
                AngleUnit.DEG -> "°"
                AngleUnit.RAD -> " rad"
                AngleUnit.GRAD -> " grad"
            }
            return "$r ∠ ${formatRealAuto(angleConverted)}$unitSuffix"
        }

        if (clean.isReal) {
            return when (mode) {
                NumberDisplayMode.AUTO -> formatRealAuto(clean.re)
                NumberDisplayMode.FRACTION -> formatAsFractionOrSymbolic(clean.re) ?: formatRealAuto(clean.re)
                NumberDisplayMode.SCIENTIFIC -> formatScientific(clean.re)
                NumberDisplayMode.POLAR -> formatRealAuto(clean.re)
            }
        }

        // Complex number formatting: a + bi
        val reStr = if (abs(clean.re) < 1e-13) "" else formatRealSingle(clean.re, mode)
        val imAbs = abs(clean.im)
        val imCoeffStr = if (abs(imAbs - 1.0) < 1e-12) "" else formatRealSingle(imAbs, mode)
        val imSign = if (clean.im < 0) "-" else "+"

        return if (reStr.isEmpty()) {
            if (clean.im < 0) "-${imCoeffStr}i" else "${imCoeffStr}i"
        } else {
            "$reStr $imSign ${imCoeffStr}i"
        }
    }

    private fun formatRealSingle(v: Double, mode: NumberDisplayMode): String {
        return when (mode) {
            NumberDisplayMode.FRACTION -> formatAsFractionOrSymbolic(v) ?: formatRealAuto(v)
            NumberDisplayMode.SCIENTIFIC -> formatScientific(v)
            else -> formatRealAuto(v)
        }
    }

    fun formatRealAuto(v: Double): String {
        if (v == 0.0 || abs(v) == 0.0) return "0"
        val absV = abs(v)
        if (absV >= 1e12 || (absV < 1e-7 && absV > 0.0)) {
            return formatScientific(v)
        }
        val rounded = v.roundToLong()
        if (absV < 1e12 && abs(v - rounded) < 1e-11 * max(1.0, absV)) {
            return addGrouping(rounded.toString())
        }
        val bd = BigDecimal.valueOf(v).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        val plain = bd.toPlainString()
        val parts = plain.split(".")
        val intPart = addGrouping(parts[0])
        return if (parts.size > 1 && parts[1].isNotEmpty()) "$intPart.${parts[1]}" else intPart
    }

    fun formatRealCompact(v: Double): String {
        if (abs(v) < 1e-13) return "0"
        val rounded = v.roundToLong()
        if (abs(v) < 1e12 && abs(v - rounded) < 1e-10 * max(1.0, abs(v))) {
            return rounded.toString()
        }
        val df = DecimalFormat("0.######", symbols)
        return df.format(v)
    }

    fun formatScientific(v: Double): String {
        if (abs(v) == 0.0) return "0"
        val exp = floor(log10(abs(v))).toInt()
        val mantissa = v / 10.0.pow(exp)
        val df = DecimalFormat("0.########", symbols)
        val mantissaStr = df.format(mantissa)
        return "$mantissaStr×10${toSuperscript(exp.toString())}"
    }

    /**
     * Converts a real number into an exact fraction (e.g. 3/4), or symbolic pi/radical multiple
     * (e.g., pi/2, sqrt(2)/2) if an exact low-denominator match exists!
     */
    fun formatAsFractionOrSymbolic(v: Double): String? {
        if (!v.isFinite()) return null
        if (abs(v) < 1e-13) return "0"

        // 1. Check simple rational a/b
        approximateFraction(v, maxDen = 10_000)?.let { (num, den) ->
            if (den == 1L) return num.toString()
            return "$num/$den"
        }

        // 2. Check rational multiple of pi: (a/b)*pi
        approximateFraction(v / PI, maxDen = 144)?.let { (num, den) ->
            val prefix = when (num) {
                1L -> "π"
                -1L -> "-π"
                else -> "${num}π"
            }
            return if (den == 1L) prefix else "$prefix/$den"
        }

        // 3. Check rational multiple of e: (a/b)*e
        approximateFraction(v / E, maxDen = 36)?.let { (num, den) ->
            val prefix = when (num) {
                1L -> "e"
                -1L -> "-e"
                else -> "${num}e"
            }
            return if (den == 1L) prefix else "$prefix/$den"
        }

        // 4. Check common square roots: (a/b)*sqrt(k) for square-free k in 2..19
        val squareFree = intArrayOf(2, 3, 5, 6, 7, 10, 11, 13, 14, 15, 17, 19)
        for (k in squareFree) {
            val rootK = sqrt(k.toDouble())
            approximateFraction(v / rootK, maxDen = 36)?.let { (num, den) ->
                val absNum = abs(num)
                val sign = if (num < 0) "-" else ""
                val numPart = if (absNum == 1L) "√$k" else "${absNum}√$k"
                return if (den == 1L) "$sign$numPart" else "$sign$numPart/$den"
            }
        }

        return null
    }

    /**
     * Generates a helpful secondary hint chip when in AUTO mode (e.g. "3/4" or "π/2" or "√2/2" or Polar).
     */
    fun getAlternateRepresentation(value: Complex, mode: NumberDisplayMode, angleUnit: AngleUnit): String? {
        val clean = value.cleanNoise()
        if (!clean.re.isFinite() || !clean.im.isFinite()) return null
        if (!clean.isReal) {
            return if (mode != NumberDisplayMode.POLAR) {
                format(clean, NumberDisplayMode.POLAR, angleUnit)
            } else {
                format(clean, NumberDisplayMode.AUTO, angleUnit)
            }
        }
        val rounded = clean.re.roundToLong()
        if (abs(clean.re) < 1e12 && abs(clean.re - rounded) < 1e-11) {
            return null // Already an exact integer
        }
        return if (mode != NumberDisplayMode.FRACTION) {
            formatAsFractionOrSymbolic(clean.re)
        } else {
            formatRealAuto(clean.re)
        }
    }

    fun approximateFraction(value: Double, maxDen: Long = 10_000L, tolerance: Double = 1e-10): Pair<Long, Long>? {
        if (!value.isFinite() || abs(value) > 1e9) return null
        val sign = if (value < 0) -1L else 1L
        val x = abs(value)
        var h1 = 1L
        var h2 = 0L
        var k1 = 0L
        var k2 = 1L
        var b = x
        for (iter in 0 until 32) {
            val a = floor(b).toLong()
            val h = a * h1 + h2
            val k = a * k1 + k2
            if (k > maxDen || h > 1_000_000_000L) break
            h2 = h1
            h1 = h
            k2 = k1
            k1 = k
            val approx = h1.toDouble() / k1.toDouble()
            if (abs(x - approx) <= tolerance * max(1.0, x)) {
                return Pair(sign * h1, k1)
            }
            val rem = b - a
            if (rem < 1e-14) break
            b = 1.0 / rem
        }
        return null
    }

    private fun addGrouping(rawInt: String): String {
        val negative = rawInt.startsWith("-")
        val digits = if (negative) rawInt.substring(1) else rawInt
        if (digits.length <= 3) return rawInt
        val sb = StringBuilder()
        var count = 0
        for (i in digits.length - 1 downTo 0) {
            sb.append(digits[i])
            count++
            if (count % 3 == 0 && i != 0) sb.append(',')
        }
        val grouped = sb.reverse().toString()
        return if (negative) "-$grouped" else grouped
    }

    fun toSuperscript(text: String): String {
        val map = mapOf(
            '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
            '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
            '-' to '⁻', '+' to '⁺'
        )
        return text.map { map[it] ?: it }.joinToString("")
    }
}
