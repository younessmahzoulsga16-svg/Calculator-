package com.example.engine

import kotlin.math.*

enum class EquationType(val title: String, val badge: String) {
    LINEAR("Linear", "ax + b = c"),
    QUADRATIC("Quadratic", "ax² + bx + c = 0"),
    CUBIC("Cubic", "ax³ + bx² + cx + d = 0"),
    SYSTEM_2X2("2×2 System", "2 Vars"),
    SYSTEM_3X3("3×3 System", "3 Vars"),
    CUSTOM_FX("Custom f(x)", "f(x) = g(x)")
}

data class SolutionItem(
    val label: String,
    val value: String,
    val alternateValue: String? = null
)

data class EquationSolution(
    val summary: String,
    val items: List<SolutionItem>,
    val steps: List<String>,
    val isError: Boolean = false
)

object EquationSolver {

    fun solveLinear(a: Double, b: Double, c: Double): EquationSolution {
        if (abs(a) < 1e-14) {
            return if (abs(b - c) < 1e-14) {
                EquationSolution("Infinite solutions (0 = 0)", emptyList(), listOf("Identity holds for all x ∈ ℝ"))
            } else {
                EquationSolution("No solution (${fmt(b)} ≠ ${fmt(c)})", emptyList(), listOf("Parallel inconsistent equation"), isError = true)
            }
        }
        val x = (c - b) / a
        val frac = NumberFormatter.formatAsFractionOrSymbolic(x)
        return EquationSolution(
            summary = "Unique real root found",
            items = listOf(SolutionItem("x", fmt(x), frac?.takeIf { it != fmt(x) })),
            steps = listOf(
                "${fmt(a)}x = ${fmt(c)} − ${fmt(b)} = ${fmt(c - b)}",
                "x = ${fmt(c - b)} / ${fmt(a)} = ${fmt(x)}"
            )
        )
    }

    fun solveQuadratic(a: Double, b: Double, c: Double): EquationSolution {
        if (abs(a) < 1e-14) return solveLinear(b, c, 0.0)
        val delta = b * b - 4.0 * a * c
        val vx = -b / (2.0 * a)
        val vy = a * vx * vx + b * vx + c
        val vertexStr = "(${fmt(vx)}, ${fmt(vy)})"

        return if (delta > 1e-13) {
            val sqrtD = sqrt(delta)
            val x1 = (-b + sqrtD) / (2.0 * a)
            val x2 = (-b - sqrtD) / (2.0 * a)
            EquationSolution(
                summary = "2 distinct real roots",
                items = listOf(
                    SolutionItem("x₁", fmt(x1), NumberFormatter.formatAsFractionOrSymbolic(x1)),
                    SolutionItem("x₂", fmt(x2), NumberFormatter.formatAsFractionOrSymbolic(x2)),
                    SolutionItem("Δ (Discriminant)", fmt(delta)),
                    SolutionItem("Vertex", vertexStr)
                ),
                steps = listOf(
                    "Δ = b² − 4ac = ${fmt(b)}² − 4(${fmt(a)})(${fmt(c)}) = ${fmt(delta)}",
                    "x = (−b ± √Δ) / 2a = (${fmt(-b)} ± ${fmt(sqrtD)}) / ${fmt(2 * a)}"
                )
            )
        } else if (abs(delta) <= 1e-13) {
            val x = -b / (2.0 * a)
            EquationSolution(
                summary = "1 repeated real root",
                items = listOf(
                    SolutionItem("x₁ = x₂", fmt(x), NumberFormatter.formatAsFractionOrSymbolic(x)),
                    SolutionItem("Δ (Discriminant)", "0"),
                    SolutionItem("Vertex", vertexStr)
                ),
                steps = listOf("Δ = 0 → double root at x = −b / 2a = ${fmt(x)}")
            )
        } else {
            val sqrtNegD = sqrt(-delta)
            val re = -b / (2.0 * a)
            val im = abs(sqrtNegD / (2.0 * a))
            val z1 = Complex(re, im)
            val z2 = Complex(re, -im)
            EquationSolution(
                summary = "2 complex conjugate roots",
                items = listOf(
                    SolutionItem("x₁", NumberFormatter.format(z1, NumberDisplayMode.AUTO)),
                    SolutionItem("x₂", NumberFormatter.format(z2, NumberDisplayMode.AUTO)),
                    SolutionItem("Δ (Discriminant)", fmt(delta)),
                    SolutionItem("Vertex", vertexStr)
                ),
                steps = listOf(
                    "Δ = ${fmt(delta)} < 0 → complex conjugate pair",
                    "x = ${fmt(re)} ± ${fmt(im)}i"
                )
            )
        }
    }

    fun solveCubic(a: Double, b: Double, c: Double, d: Double): EquationSolution {
        if (abs(a) < 1e-14) return solveQuadratic(b, c, d)
        // Depressed cubic t^3 + p t + q = 0 where x = t - b/(3a)
        val p = (3.0 * a * c - b * b) / (3.0 * a * a)
        val q = (2.0 * b * b * b - 9.0 * a * b * c + 27.0 * a * a * d) / (27.0 * a * a * a)
        val shift = -b / (3.0 * a)
        val disc = (q * q) / 4.0 + (p * p * p) / 27.0

        val roots = mutableListOf<Complex>()
        if (disc > 1e-12) {
            val sqrtDisc = sqrt(disc)
            val u = cbrt(-q / 2.0 + sqrtDisc)
            val v = cbrt(-q / 2.0 - sqrtDisc)
            val t1 = u + v
            val realPart = -(u + v) / 2.0 + shift
            val imagPart = (u - v) * sqrt(3.0) / 2.0
            roots.add(Complex(t1 + shift, 0.0))
            roots.add(Complex(realPart, imagPart))
            roots.add(Complex(realPart, -imagPart))
        } else if (abs(disc) <= 1e-12) {
            if (abs(p) < 1e-12 && abs(q) < 1e-12) {
                repeat(3) { roots.add(Complex(shift, 0.0)) }
            } else {
                val u = cbrt(-q / 2.0)
                roots.add(Complex(2.0 * u + shift, 0.0))
                roots.add(Complex(-u + shift, 0.0))
                roots.add(Complex(-u + shift, 0.0))
            }
        } else {
            // Three distinct real roots via trigonometric method
            val r = sqrt(-p * p * p / 27.0)
            val phi = acos(max(-1.0, min(1.0, -q / (2.0 * r))))
            val m = 2.0 * sqrt(-p / 3.0)
            for (k in 0..2) {
                val t = m * cos((phi + 2.0 * PI * k) / 3.0)
                roots.add(Complex(t + shift, 0.0))
            }
        }

        val realCount = roots.count { it.isReal }
        return EquationSolution(
            summary = if (realCount == 3) "3 real roots" else "1 real & 2 complex roots",
            items = roots.mapIndexed { index, z ->
                val formatted = NumberFormatter.format(z, NumberDisplayMode.AUTO)
                val frac = if (z.isReal) NumberFormatter.formatAsFractionOrSymbolic(z.re) else null
                SolutionItem("x${NumberFormatter.toSuperscript((index + 1).toString()).replace('¹', '₁').replace('²', '₂').replace('³', '₃')}", formatted, frac)
            },
            steps = listOf(
                "Depressed cubic: t³ + (${fmt(p)})t + (${fmt(q)}) = 0",
                "Substitution shift: x = t ${if (shift >= 0) "+" else "−"} ${fmt(abs(shift))}"
            )
        )
    }

    fun solve2x2(
        a1: Double, b1: Double, c1: Double,
        a2: Double, b2: Double, c2: Double
    ): EquationSolution {
        val det = a1 * b2 - a2 * b1
        if (abs(det) < 1e-14) {
            val detX = c1 * b2 - c2 * b1
            val msg = if (abs(detX) < 1e-14) "Infinite solutions (dependent)" else "No solution (inconsistent)"
            return EquationSolution(msg, listOf(SolutionItem("Determinant", "0")), listOf("Δ = a₁b₂ − a₂b₁ = 0"), isError = abs(detX) >= 1e-14)
        }
        val x = (c1 * b2 - c2 * b1) / det
        val y = (a1 * c2 - a2 * c1) / det
        return EquationSolution(
            summary = "Unique system solution",
            items = listOf(
                SolutionItem("x", fmt(x), NumberFormatter.formatAsFractionOrSymbolic(x)),
                SolutionItem("y", fmt(y), NumberFormatter.formatAsFractionOrSymbolic(y)),
                SolutionItem("det(A)", fmt(det))
            ),
            steps = listOf(
                "det(A) = (${fmt(a1)})(${fmt(b2)}) − (${fmt(a2)})(${fmt(b1)}) = ${fmt(det)}",
                "Cramer's Rule → x = ${fmt(x)}, y = ${fmt(y)}"
            )
        )
    }

    fun solve3x3(matrix: Array<DoubleArray>, rhs: DoubleArray): EquationSolution {
        val a = Array(3) { r -> matrix[r].copyOf() }
        val b = rhs.copyOf()
        val det = determinant3x3(matrix)
        if (abs(det) < 1e-13) {
            return EquationSolution(
                summary = "Singular matrix (det = 0)",
                items = listOf(SolutionItem("det(A)", "0")),
                steps = listOf("Matrix is singular: either no solution or infinitely many solutions."),
                isError = true
            )
        }
        // Gaussian elimination with partial pivoting
        for (col in 0 until 3) {
            var pivot = col
            for (row in col + 1 until 3) {
                if (abs(a[row][col]) > abs(a[pivot][col])) pivot = row
            }
            val tmpRow = a[col]; a[col] = a[pivot]; a[pivot] = tmpRow
            val tmpB = b[col]; b[col] = b[pivot]; b[pivot] = tmpB

            for (row in col + 1 until 3) {
                val factor = a[row][col] / a[col][col]
                for (k in col until 3) a[row][k] -= factor * a[col][k]
                b[row] -= factor * b[col]
            }
        }
        val sol = DoubleArray(3)
        for (row in 2 downTo 0) {
            var sum = b[row]
            for (k in row + 1 until 3) sum -= a[row][k] * sol[k]
            sol[row] = sum / a[row][row]
        }
        return EquationSolution(
            summary = "Unique 3×3 solution",
            items = listOf(
                SolutionItem("x", fmt(sol[0]), NumberFormatter.formatAsFractionOrSymbolic(sol[0])),
                SolutionItem("y", fmt(sol[1]), NumberFormatter.formatAsFractionOrSymbolic(sol[1])),
                SolutionItem("z", fmt(sol[2]), NumberFormatter.formatAsFractionOrSymbolic(sol[2])),
                SolutionItem("det(A)", fmt(det))
            ),
            steps = listOf(
                "det(A) = ${fmt(det)}",
                "Gaussian elimination with partial pivoting completed."
            )
        )
    }

    /**
     * Solves arbitrary equations like "x^2 - 5x + 6 = 0" or "sin(x) = 0.5"
     * using multi-start Newton-Raphson + secant bracketing.
     */
    fun solveCustomEquation(
        equationText: String,
        initialGuess: Double = 1.0,
        angleUnit: AngleUnit = AngleUnit.RAD
    ): EquationSolution {
        val parts = equationText.split("=")
        val lhs = parts.getOrNull(0)?.trim().orEmpty()
        val rhs = parts.getOrNull(1)?.trim().orEmpty()
        if (lhs.isEmpty()) {
            return EquationSolution("Enter an equation with 'x'", emptyList(), emptyList(), isError = true)
        }
        val combined = if (rhs.isEmpty()) lhs else "($lhs)-($rhs)"

        fun evalF(x: Double): Double? = try {
            val res = MathExpressionEngine.evaluate(combined, angleUnit, xValue = Complex(x, 0.0))
            if (res.isReal && res.re.isFinite()) res.re else null
        } catch (_: Exception) {
            null
        }

        val foundRoots = mutableListOf<Double>()
        val seeds = listOf(initialGuess, 0.0, 1.0, -1.0, 2.0, -2.0, 5.0, -5.0, 10.0, -10.0, 0.5, -0.5)
        for (seed in seeds) {
            var x = seed
            var converged = false
            for (iter in 0 until 80) {
                val fx = evalF(x) ?: break
                if (abs(fx) < 1e-11) {
                    converged = true
                    break
                }
                val h = max(1e-7, abs(x) * 1e-7)
                val fPlus = evalF(x + h)
                val fMinus = evalF(x - h)
                if (fPlus == null || fMinus == null) break
                val dfx = (fPlus - fMinus) / (2.0 * h)
                if (abs(dfx) < 1e-14) break
                val nextX = x - fx / dfx
                if (!nextX.isFinite() || abs(nextX) > 1e12) break
                if (abs(nextX - x) < 1e-12) {
                    val fCheck = evalF(nextX)
                    if (fCheck != null && abs(fCheck) < 1e-7) {
                        x = nextX
                        converged = true
                    }
                    break
                }
                x = nextX
            }
            if (converged) {
                val cleanX = if (abs(x) < 1e-11) 0.0 else x
                if (foundRoots.none { abs(it - cleanX) < 1e-5 }) {
                    foundRoots.add(cleanX)
                }
            }
        }

        if (foundRoots.isEmpty()) {
            return EquationSolution(
                summary = "No real root found near guesses",
                items = emptyList(),
                steps = listOf("Try adjusting the initial guess x₀ or check syntax (e.g. x^2 - 4 = 0)."),
                isError = true
            )
        }
        val sorted = foundRoots.sorted()
        return EquationSolution(
            summary = "Found ${sorted.size} real root${if (sorted.size > 1) "s" else ""}",
            items = sorted.mapIndexed { idx, r ->
                val residual = evalF(r) ?: 0.0
                SolutionItem(
                    label = if (sorted.size == 1) "x" else "x${idx + 1}",
                    value = fmt(r),
                    alternateValue = NumberFormatter.formatAsFractionOrSymbolic(r) ?: "f(x) ≈ ${fmt(residual)}"
                )
            },
            steps = listOf(
                "Evaluated f(x) = $combined = 0 (${angleUnit.label} mode)",
                "Converged via multi-seed Newton-Raphson solver"
            )
        )
    }

    private fun determinant3x3(m: Array<DoubleArray>): Double {
        return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) -
            m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
            m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])
    }

    private fun fmt(v: Double): String = NumberFormatter.formatRealAuto(v)
}
