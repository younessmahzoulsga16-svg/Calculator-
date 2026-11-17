package com.example.engine

import kotlin.math.*

/**
 * High-speed recursive-descent mathematical expression parser & evaluator.
 * Supports real & complex numbers, implicit multiplication, scientific notation,
 * constants (pi, e, phi, i, Ans), variable x, trig/hyperbolic/log/factorial/combinatorics.
 */
object MathExpressionEngine {

    private sealed class Token {
        data class Num(val value: Complex) : Token()
        data class Ident(val name: String) : Token()
        data class Op(val symbol: String) : Token()
        data object LParen : Token()
        data object RParen : Token()
        data object Comma : Token()
    }

    fun evaluate(
        expression: String,
        angleUnit: AngleUnit = AngleUnit.DEG,
        ans: Complex = Complex.ZERO,
        xValue: Complex? = null
    ): Complex {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return Complex.ZERO
        val tokens = tokenize(trimmed)
        if (tokens.isEmpty()) return Complex.ZERO
        val normalized = insertImplicitMultiplication(tokens)
        val parser = Parser(normalized, angleUnit, ans, xValue)
        val result = parser.parseExpression()
        if (parser.hasMore()) {
            throw MathEvalException("Unexpected syntax near '${parser.peekDescribe()}'")
        }
        return result.cleanNoise()
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val n = input.length

        while (i < n) {
            val c = input[i]
            when {
                c.isWhitespace() || c == ',' -> {
                    // Skip thousand separators if comma isn't used as decimal; treat comma as argument separator only if inside func?
                    // Note: we use ',' exclusively as display grouping or arg separator if needed. Let's skip whitespace and keep ',' as Comma only when not digit grouping.
                    if (c == ',') tokens.add(Token.Comma)
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val start = i
                    var hasDot = (c == '.')
                    i++
                    while (i < n && (input[i].isDigit() || (!hasDot && input[i] == '.'))) {
                        if (input[i] == '.') hasDot = true
                        i++
                    }
                    // Handle scientific notation like 1.5E-4 or 2e+3 (but NOT 2e where e is Euler's number!)
                    if (i < n && (input[i] == 'E' || (input[i] == 'e' && i + 1 < n && (input[i + 1] == '+' || input[i + 1] == '-' || input[i + 1].isDigit())))) {
                        val expStart = i
                        i++
                        if (i < n && (input[i] == '+' || input[i] == '-')) i++
                        if (i < n && input[i].isDigit()) {
                            while (i < n && input[i].isDigit()) i++
                        } else {
                            i = expStart
                        }
                    }
                    val numStr = input.substring(start, i)
                    val parsed = numStr.toDoubleOrNull()
                        ?: throw MathEvalException("Invalid number '$numStr'")
                    tokens.add(Token.Num(Complex(parsed, 0.0)))
                }
                c == 'π' -> { tokens.add(Token.Ident("pi")); i++ }
                c == 'φ' -> { tokens.add(Token.Ident("phi")); i++ }
                c == '√' -> { tokens.add(Token.Ident("sqrt")); i++ }
                c == '∛' -> { tokens.add(Token.Ident("cbrt")); i++ }
                c == '×' || c == '*' -> { tokens.add(Token.Op("*")); i++ }
                c == '÷' || c == '/' -> { tokens.add(Token.Op("/")); i++ }
                c == '−' || c == '-' -> { tokens.add(Token.Op("-")); i++ }
                c == '+' -> { tokens.add(Token.Op("+")); i++ }
                c == '^' -> { tokens.add(Token.Op("^")); i++ }
                c == '%' -> { tokens.add(Token.Op("%")); i++ }
                c == '!' -> { tokens.add(Token.Op("!")); i++ }
                c == '(' -> { tokens.add(Token.LParen); i++ }
                c == ')' -> { tokens.add(Token.RParen); i++ }
                c == '²' -> {
                    tokens.add(Token.Op("^"))
                    tokens.add(Token.Num(Complex(2.0, 0.0)))
                    i++
                }
                c == '³' -> {
                    tokens.add(Token.Op("^"))
                    tokens.add(Token.Num(Complex(3.0, 0.0)))
                    i++
                }
                c.isLetter() -> {
                    val start = i
                    i++
                    while (i < n && (input[i].isLetterOrDigit() || input[i] == '_' || input[i] == '⁻' || input[i] == '¹')) {
                        i++
                    }
                    // Normalize inverse trig suffixes like sin⁻¹
                    var ident = input.substring(start, i)
                    ident = ident.replace("⁻¹", "_inv")
                    // Check for known multi-letter tokens or split single-letter constants like 2ix
                    when (ident.lowercase()) {
                        "mod" -> tokens.add(Token.Op("mod"))
                        "ncr" -> tokens.add(Token.Op("nCr"))
                        "npr" -> tokens.add(Token.Op("nPr"))
                        else -> tokens.add(Token.Ident(ident))
                    }
                }
                else -> throw MathEvalException("Unsupported symbol '$c'")
            }
        }
        return tokens
    }

    private fun isValueEnd(t: Token): Boolean = when (t) {
        is Token.Num, is Token.RParen -> true
        is Token.Op -> t.symbol == "!" || t.symbol == "%"
        is Token.Ident -> !isFunctionName(t.name)
        else -> false
    }

    private fun isValueStart(t: Token): Boolean = when (t) {
        is Token.Num, is Token.LParen, is Token.Ident -> true
        else -> false
    }

    private fun isFunctionName(name: String): Boolean {
        return when (name.lowercase()) {
            "sin", "cos", "tan", "csc", "sec", "cot",
            "asin", "acos", "atan", "sin_inv", "cos_inv", "tan_inv",
            "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
            "sinh_inv", "cosh_inv", "tanh_inv",
            "ln", "log", "log10", "log2", "sqrt", "cbrt", "abs",
            "exp", "floor", "ceil", "round", "re", "im", "arg", "conj", "fact" -> true
            else -> false
        }
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.size < 2) return tokens
        val out = ArrayList<Token>(tokens.size + 4)
        for (idx in tokens.indices) {
            val curr = tokens[idx]
            if (out.isNotEmpty()) {
                val prev = out.last()
                if (isValueEnd(prev) && isValueStart(curr)) {
                    out.add(Token.Op("*"))
                }
            }
            out.add(curr)
        }
        return out
    }

    private class Parser(
        private val tokens: List<Token>,
        private val angleUnit: AngleUnit,
        private val ans: Complex,
        private val xValue: Complex?
    ) {
        private var pos = 0

        fun hasMore(): Boolean = pos < tokens.size

        fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null

        fun peekDescribe(): String = peek()?.toString() ?: "end"

        private fun consume(): Token = tokens[pos++]

        fun parseExpression(): Complex = parseAddSub()

        private fun parseAddSub(): Complex {
            var left = parseMulDiv()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == "+" || t.symbol == "-")) {
                    consume()
                    val right = parseMulDiv()
                    left = if (t.symbol == "+") left + right else left - right
                } else break
            }
            return left
        }

        private fun parseMulDiv(): Complex {
            var left = parseCombPerm()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == "*" || t.symbol == "/" || t.symbol == "mod")) {
                    consume()
                    val right = parseCombPerm()
                    left = when (t.symbol) {
                        "*" -> left * right
                        "/" -> left / right
                        else -> modulo(left, right)
                    }
                } else break
            }
            return left
        }

        private fun parseCombPerm(): Complex {
            var left = parseUnary()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == "nCr" || t.symbol == "nPr")) {
                    consume()
                    val right = parseUnary()
                    left = if (t.symbol == "nCr") nCr(left, right) else nPr(left, right)
                } else break
            }
            return left
        }

        private fun parseUnary(): Complex {
            val t = peek()
            if (t is Token.Op && (t.symbol == "+" || t.symbol == "-")) {
                consume()
                val operand = parseUnary()
                return if (t.symbol == "-") -operand else operand
            }
            return parsePower()
        }

        private fun parsePower(): Complex {
            val base = parsePostfix()
            val t = peek()
            if (t is Token.Op && t.symbol == "^") {
                consume()
                val exponent = parseUnary() // right-associative, allows 2^-3
                return base.pow(exponent)
            }
            return base
        }

        private fun parsePostfix(): Complex {
            var value = parsePrimary()
            while (true) {
                val t = peek()
                if (t is Token.Op && t.symbol == "!") {
                    consume()
                    value = factorial(value)
                } else if (t is Token.Op && t.symbol == "%") {
                    consume()
                    value = value / Complex(100.0, 0.0)
                } else break
            }
            return value
        }

        private fun parsePrimary(): Complex {
            val t = peek() ?: throw MathEvalException("Incomplete expression")
            return when (t) {
                is Token.Num -> {
                    consume()
                    t.value
                }
                is Token.LParen -> {
                    consume()
                    val inner = parseExpression()
                    val close = peek()
                    if (close is Token.RParen) {
                        consume()
                    }
                    // Auto-close unclosed trailing parenthesis for fast live preview
                    inner
                }
                is Token.Ident -> {
                    consume()
                    if (isFunctionName(t.name)) {
                        val arg = if (peek() is Token.LParen) {
                            consume()
                            val inner = parseExpression()
                            if (peek() is Token.RParen) consume()
                            inner
                        } else {
                            parseUnary()
                        }
                        applyFunction(t.name.lowercase(), arg)
                    } else {
                        resolveConstant(t.name)
                    }
                }
                else -> throw MathEvalException("Unexpected token")
            }
        }

        private fun resolveConstant(name: String): Complex {
            return when (name.lowercase()) {
                "pi" -> Complex(PI, 0.0)
                "e" -> Complex(E, 0.0)
                "phi" -> Complex((1.0 + sqrt(5.0)) / 2.0, 0.0)
                "i" -> Complex.I
                "ans" -> ans
                "x" -> xValue ?: throw MathEvalException("Variable 'x' requires Equation Solver")
                else -> throw MathEvalException("Unknown symbol '$name'")
            }
        }

        private fun toRadians(angle: Complex): Complex {
            return when (angleUnit) {
                AngleUnit.RAD -> angle
                AngleUnit.DEG -> angle * Complex(PI / 180.0, 0.0)
                AngleUnit.GRAD -> angle * Complex(PI / 200.0, 0.0)
            }
        }

        private fun fromRadians(rad: Complex): Complex {
            return when (angleUnit) {
                AngleUnit.RAD -> rad
                AngleUnit.DEG -> rad * Complex(180.0 / PI, 0.0)
                AngleUnit.GRAD -> rad * Complex(200.0 / PI, 0.0)
            }
        }

        private fun applyFunction(fn: String, arg: Complex): Complex {
            return when (fn) {
                "sin" -> trigSin(arg)
                "cos" -> trigCos(arg)
                "tan" -> trigTan(arg)
                "csc" -> Complex.ONE / trigSin(arg)
                "sec" -> Complex.ONE / trigCos(arg)
                "cot" -> trigCos(arg) / trigSin(arg)
                "asin", "sin_inv" -> fromRadians(arg.asin())
                "acos", "cos_inv" -> fromRadians(arg.acos())
                "atan", "tan_inv" -> fromRadians(arg.atan())
                "sinh" -> arg.sinh()
                "cosh" -> arg.cosh()
                "tanh" -> arg.tanh()
                "asinh", "sinh_inv" -> arg.asinh()
                "acosh", "cosh_inv" -> arg.acosh()
                "atanh", "tanh_inv" -> arg.atanh()
                "ln" -> arg.ln()
                "log", "log10" -> arg.log10()
                "log2" -> arg.log2()
                "sqrt" -> arg.sqrt()
                "cbrt" -> arg.cbrt()
                "abs" -> Complex(arg.absValue, 0.0)
                "exp" -> arg.exp()
                "floor" -> if (arg.isReal) Complex(floor(arg.re), 0.0) else Complex(floor(arg.re), floor(arg.im))
                "ceil" -> if (arg.isReal) Complex(ceil(arg.re), 0.0) else Complex(ceil(arg.re), ceil(arg.im))
                "round" -> if (arg.isReal) Complex(round(arg.re), 0.0) else Complex(round(arg.re), round(arg.im))
                "re" -> Complex(arg.re, 0.0)
                "im" -> Complex(arg.im, 0.0)
                "arg" -> fromRadians(Complex(arg.argRadians, 0.0))
                "conj" -> arg.conj()
                "fact" -> factorial(arg)
                else -> throw MathEvalException("Unknown function '$fn'")
            }
        }

        private fun trigSin(arg: Complex): Complex {
            if (arg.isReal && angleUnit == AngleUnit.DEG) {
                val deg = ((arg.re % 360.0) + 360.0) % 360.0
                if (abs(deg) < 1e-12 || abs(deg - 180.0) < 1e-12 || abs(deg - 360.0) < 1e-12) return Complex.ZERO
                if (abs(deg - 90.0) < 1e-12) return Complex.ONE
                if (abs(deg - 270.0) < 1e-12) return Complex(-1.0, 0.0)
                if (abs(deg - 30.0) < 1e-12 || abs(deg - 150.0) < 1e-12) return Complex(0.5, 0.0)
                if (abs(deg - 210.0) < 1e-12 || abs(deg - 330.0) < 1e-12) return Complex(-0.5, 0.0)
            }
            return toRadians(arg).sin()
        }

        private fun trigCos(arg: Complex): Complex {
            if (arg.isReal && angleUnit == AngleUnit.DEG) {
                val deg = ((arg.re % 360.0) + 360.0) % 360.0
                if (abs(deg - 90.0) < 1e-12 || abs(deg - 270.0) < 1e-12) return Complex.ZERO
                if (abs(deg) < 1e-12 || abs(deg - 360.0) < 1e-12) return Complex.ONE
                if (abs(deg - 180.0) < 1e-12) return Complex(-1.0, 0.0)
                if (abs(deg - 60.0) < 1e-12 || abs(deg - 300.0) < 1e-12) return Complex(0.5, 0.0)
                if (abs(deg - 120.0) < 1e-12 || abs(deg - 240.0) < 1e-12) return Complex(-0.5, 0.0)
            }
            return toRadians(arg).cos()
        }

        private fun trigTan(arg: Complex): Complex {
            val c = trigCos(arg)
            if (c.absValue < 1e-12) throw MathEvalException("Undefined tangent")
            return trigSin(arg) / c
        }

        private fun modulo(a: Complex, b: Complex): Complex {
            if (!a.isReal || !b.isReal) throw MathEvalException("Modulo requires real numbers")
            if (abs(b.re) < 1e-15) throw MathEvalException("Modulo by zero")
            val r = a.re % b.re
            return Complex(if (r < 0) r + abs(b.re) else r, 0.0)
        }

        private fun factorial(arg: Complex): Complex {
            if (!arg.isReal) throw MathEvalException("Factorial requires a real number")
            val v = arg.re
            if (v < 0 && abs(v - round(v)) < 1e-12) throw MathEvalException("Undefined for negative integers")
            if (v > 170.0) throw MathEvalException("Overflow (> 170!)")
            val rounded = round(v)
            if (abs(v - rounded) < 1e-12 && rounded >= 0) {
                var acc = 1.0
                for (k in 2..rounded.toInt()) acc *= k
                return Complex(acc, 0.0)
            }
            // Lanczos approximation for Gamma(v + 1)
            return Complex(gamma(v + 1.0), 0.0)
        }

        private fun gamma(z: Double): Double {
            val p = doubleArrayOf(
                676.5203681218851, -1259.1392167224028, 771.32342877765313,
                -176.61502916214059, 12.507343278686905, -0.13857109526572012,
                9.9843695780195716e-6, 1.5056327351493116e-7
            )
            if (z < 0.5) {
                return PI / (sin(PI * z) * gamma(1.0 - z))
            }
            val z1 = z - 1.0
            var x = 0.99999999999980993
            for (i in p.indices) x += p[i] / (z1 + i + 1)
            val t = z1 + p.size - 0.5
            return sqrt(2.0 * PI) * t.pow(z1 + 0.5) * exp(-t) * x
        }

        private fun nCr(n: Complex, r: Complex): Complex {
            if (!n.isReal || !r.isReal) throw MathEvalException("nCr requires real integers")
            val ni = n.re.roundToInt()
            val ri = r.re.roundToInt()
            if (abs(n.re - ni) > 1e-9 || abs(r.re - ri) > 1e-9 || ni < 0 || ri < 0 || ri > ni) {
                throw MathEvalException("Invalid nCr domain (0 ≤ r ≤ n)")
            }
            val k = min(ri, ni - ri)
            var res = 1.0
            for (i in 1..k) {
                res = res * (ni - k + i) / i
            }
            return Complex(res, 0.0)
        }

        private fun nPr(n: Complex, r: Complex): Complex {
            if (!n.isReal || !r.isReal) throw MathEvalException("nPr requires real integers")
            val ni = n.re.roundToInt()
            val ri = r.re.roundToInt()
            if (abs(n.re - ni) > 1e-9 || abs(r.re - ri) > 1e-9 || ni < 0 || ri < 0 || ri > ni) {
                throw MathEvalException("Invalid nPr domain (0 ≤ r ≤ n)")
            }
            var res = 1.0
            for (i in 0 until ri) {
                res *= (ni - i)
            }
            return Complex(res, 0.0)
        }
    }
}
