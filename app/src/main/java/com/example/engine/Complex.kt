package com.example.engine

import kotlin.math.*

data class Complex(val re: Double, val im: Double = 0.0) {
    val isReal: Boolean get() = abs(im) < 1e-12
    val isZero: Boolean get() = abs(re) < 1e-15 && abs(im) < 1e-15
    val absValue: Double get() = hypot(re, im)
    val argRadians: Double get() = atan2(im, re)

    operator fun plus(other: Complex): Complex = Complex(re + other.re, im + other.im)
    operator fun minus(other: Complex): Complex = Complex(re - other.re, im - other.im)
    operator fun unaryMinus(): Complex = Complex(-re, -im)

    operator fun times(other: Complex): Complex =
        Complex(re * other.re - im * other.im, re * other.im + im * other.re)

    operator fun div(other: Complex): Complex {
        val denom = other.re * other.re + other.im * other.im
        if (denom == 0.0 || denom.isNaN()) throw MathEvalException("Division by zero")
        return Complex(
            (re * other.re + im * other.im) / denom,
            (im * other.re - re * other.im) / denom
        )
    }

    fun conj(): Complex = Complex(re, -im)

    fun pow(exponent: Complex): Complex {
        if (this.isZero) {
            if (exponent.isZero) return ONE
            if (exponent.re > 0 && exponent.isReal) return ZERO
            throw MathEvalException("Undefined 0^z")
        }
        // Fast exact path for real base and integer exponent
        if (isReal && exponent.isReal) {
            val expInt = exponent.re.roundToLong()
            if (abs(exponent.re - expInt) < 1e-12 && abs(expInt) <= 1_000_000) {
                val r = re.pow(exponent.re)
                if (r.isFinite()) return Complex(r, 0.0)
            }
            if (re > 0) {
                return Complex(re.pow(exponent.re), 0.0)
            }
        }
        // General complex power: exp(exponent * ln(this))
        val lnThis = this.ln()
        return (exponent * lnThis).exp()
    }

    fun sqrt(): Complex {
        if (isReal && re >= 0.0) return Complex(kotlin.math.sqrt(re), 0.0)
        if (isReal && re < 0.0) return Complex(0.0, kotlin.math.sqrt(-re))
        val r = absValue
        val realPart = kotlin.math.sqrt((r + re) / 2.0)
        val imagPart = sign(if (im == 0.0) 1.0 else im) * kotlin.math.sqrt((r - re) / 2.0)
        return Complex(realPart, imagPart)
    }

    fun cbrt(): Complex {
        if (isReal) return Complex(kotlin.math.cbrt(re), 0.0)
        return pow(Complex(1.0 / 3.0, 0.0))
    }

    fun exp(): Complex {
        val expRe = kotlin.math.exp(re)
        return Complex(expRe * kotlin.math.cos(im), expRe * kotlin.math.sin(im)).cleanNoise()
    }

    fun ln(): Complex {
        if (isZero) throw MathEvalException("ln(0) is undefined")
        return Complex(kotlin.math.ln(absValue), argRadians)
    }

    fun log10(): Complex = ln() / Complex(kotlin.math.ln(10.0), 0.0)
    fun log2(): Complex = ln() / Complex(kotlin.math.ln(2.0), 0.0)

    fun sin(): Complex =
        Complex(kotlin.math.sin(re) * kotlin.math.cosh(im), kotlin.math.cos(re) * kotlin.math.sinh(im)).cleanNoise()

    fun cos(): Complex =
        Complex(kotlin.math.cos(re) * kotlin.math.cosh(im), -kotlin.math.sin(re) * kotlin.math.sinh(im)).cleanNoise()

    fun tan(): Complex {
        val c = cos()
        if (c.absValue < 1e-13) throw MathEvalException("Undefined tangent")
        return sin() / c
    }

    fun sinh(): Complex =
        Complex(kotlin.math.sinh(re) * kotlin.math.cos(im), kotlin.math.cosh(re) * kotlin.math.sin(im)).cleanNoise()

    fun cosh(): Complex =
        Complex(kotlin.math.cosh(re) * kotlin.math.cos(im), kotlin.math.sinh(re) * kotlin.math.sin(im)).cleanNoise()

    fun tanh(): Complex {
        val c = cosh()
        if (c.absValue < 1e-13) throw MathEvalException("Undefined tanh")
        return sinh() / c
    }

    fun asin(): Complex {
        if (isReal && abs(re) <= 1.0) return Complex(kotlin.math.asin(re), 0.0)
        val inner = (ONE - this * this).sqrt()
        return -I * (I * this + inner).ln()
    }

    fun acos(): Complex {
        if (isReal && abs(re) <= 1.0) return Complex(kotlin.math.acos(re), 0.0)
        return Complex(PI / 2.0, 0.0) - asin()
    }

    fun atan(): Complex {
        if (isReal) return Complex(kotlin.math.atan(re), 0.0)
        if (re == 0.0 && abs(abs(im) - 1.0) < 1e-12) throw MathEvalException("Undefined atan(±i)")
        return (I / Complex(2.0, 0.0)) * ((I + this) / (I - this)).ln()
    }

    fun asinh(): Complex = (this + (this * this + ONE).sqrt()).ln()

    fun acosh(): Complex {
        if (isReal && re >= 1.0) return Complex(kotlin.math.acosh(re), 0.0)
        return (this + (this - ONE).sqrt() * (this + ONE).sqrt()).ln()
    }

    fun atanh(): Complex {
        if (isReal && abs(re) >= 1.0) throw MathEvalException("Domain error for atanh")
        return Complex(0.5, 0.0) * ((ONE + this) / (ONE - this)).ln()
    }

    fun cleanNoise(): Complex {
        val scale = max(abs(re), abs(im))
        val cleanRe = if (abs(re) < 1e-13 && scale >= 1e-6) 0.0 else if (abs(re) < 1e-15) 0.0 else re
        val cleanIm = if (abs(im) < 1e-13 && scale >= 1e-6) 0.0 else if (abs(im) < 1e-15) 0.0 else im
        // Also snap values extremely close to non-zero integers (e.g., 1.0000000000000002 -> 1.0)
        val snappedRe = snapNearInt(cleanRe)
        val snappedIm = snapNearInt(cleanIm)
        return Complex(snappedRe, snappedIm)
    }

    private fun snapNearInt(v: Double): Double {
        if (!v.isFinite()) return v
        val rounded = v.roundToLong()
        return if (abs(rounded) < 1_000_000_000L && abs(v - rounded) < 1e-12 * max(1.0, abs(v))) {
            rounded.toDouble()
        } else v
    }

    companion object {
        val ZERO = Complex(0.0, 0.0)
        val ONE = Complex(1.0, 0.0)
        val I = Complex(0.0, 1.0)
    }
}

class MathEvalException(message: String) : IllegalArgumentException(message)
