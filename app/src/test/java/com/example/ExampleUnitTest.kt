package com.example

import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger
import kotlin.math.PI
import kotlin.math.abs

class ExampleUnitTest {

    @Test
    fun `arithmetic and implicit multiplication evaluate accurately`() {
        val r1 = MathExpressionEngine.evaluate("2+3×4")
        assertEquals(14.0, r1.re, 1e-9)

        val r2 = MathExpressionEngine.evaluate("2π")
        assertEquals(2.0 * PI, r2.re, 1e-9)

        val r3 = MathExpressionEngine.evaluate("(2+3)(4-1)")
        assertEquals(15.0, r3.re, 1e-9)
    }

    @Test
    fun `complex arithmetic and Euler identity evaluate accurately`() {
        val r1 = MathExpressionEngine.evaluate("√(-16)")
        assertEquals(0.0, r1.re, 1e-9)
        assertEquals(4.0, r1.im, 1e-9)

        val r2 = MathExpressionEngine.evaluate("e^(i×π)+1", AngleUnit.RAD)
        assertTrue(r2.isZero)

        val r3 = MathExpressionEngine.evaluate("(3+4i)(1-2i)")
        assertEquals(11.0, r3.re, 1e-9)
        assertEquals(-2.0, r3.im, 1e-9)
    }

    @Test
    fun `trig factorial combinatorics and fractions evaluate accurately`() {
        val sin30 = MathExpressionEngine.evaluate("sin(30)", AngleUnit.DEG)
        assertEquals(0.5, sin30.re, 1e-9)

        val fact5 = MathExpressionEngine.evaluate("5!")
        assertEquals(120.0, fact5.re, 1e-9)

        val comb = MathExpressionEngine.evaluate("10 nCr 3")
        assertEquals(120.0, comb.re, 1e-9)

        assertEquals("3/4", NumberFormatter.formatAsFractionOrSymbolic(0.75))
        assertEquals("π/2", NumberFormatter.formatAsFractionOrSymbolic(PI / 2.0))
        assertEquals("√2/2", NumberFormatter.formatAsFractionOrSymbolic(kotlin.math.sqrt(2.0) / 2.0))
    }

    @Test
    fun `equation solver solves quadratic cubic and systems`() {
        val quad = EquationSolver.solveQuadratic(1.0, -5.0, 6.0)
        assertEquals("3", quad.items[0].value)
        assertEquals("2", quad.items[1].value)

        val quadComplex = EquationSolver.solveQuadratic(1.0, 0.0, 4.0)
        assertTrue(quadComplex.summary.contains("complex"))

        val sys2 = EquationSolver.solve2x2(2.0, 3.0, 13.0, 5.0, -1.0, 7.0)
        assertEquals("2", sys2.items[0].value)
        assertEquals("3", sys2.items[1].value)

        val custom = EquationSolver.solveCustomEquation("x^2 - 9 = 0", 2.0, AngleUnit.RAD)
        assertTrue(custom.items.any { abs(it.value.toDouble() - 3.0) < 1e-6 })
    }

    @Test
    fun `base converter and bitwise operations work`() {
        val v = NumberBaseConverter.parse("FF", NumberBase.HEX)
        assertEquals(BigInteger.valueOf(255), v)
        assertEquals("1111 1111", NumberBaseConverter.format(v!!, NumberBase.BIN))
        val xor = NumberBaseConverter.applyBitwise(BigInteger.valueOf(0b1010), BigInteger.valueOf(0b1100), BitwiseOp.XOR)
        assertEquals(BigInteger.valueOf(0b0110), xor)
    }
}
