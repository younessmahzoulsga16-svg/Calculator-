package com.example.engine

import java.math.BigInteger

enum class NumberBase(val label: String, val radix: Int) {
    HEX("HEX", 16),
    DEC("DEC", 10),
    OCT("OCT", 8),
    BIN("BIN", 2)
}

enum class BitwiseOp(val symbol: String) {
    AND("AND"),
    OR("OR"),
    XOR("XOR"),
    SHL("<<"),
    SHR(">>")
}

object NumberBaseConverter {

    fun parse(raw: String, base: NumberBase): BigInteger? {
        val clean = raw.trim().replace(" ", "")
        if (clean.isEmpty() || clean == "-") return BigInteger.ZERO
        return try {
            BigInteger(clean, base.radix)
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun format(value: BigInteger, base: NumberBase): String {
        return when (base) {
            NumberBase.HEX -> groupFromRight(value.toString(16).uppercase(), 4)
            NumberBase.DEC -> value.toString(10)
            NumberBase.OCT -> groupFromRight(value.toString(8), 3)
            NumberBase.BIN -> groupFromRight(value.toString(2), 4)
        }
    }

    fun applyBitwise(a: BigInteger, b: BigInteger, op: BitwiseOp): BigInteger {
        return when (op) {
            BitwiseOp.AND -> a.and(b)
            BitwiseOp.OR -> a.or(b)
            BitwiseOp.XOR -> a.xor(b)
            BitwiseOp.SHL -> a.shiftLeft(b.toInt().coerceIn(0, 64))
            BitwiseOp.SHR -> a.shiftRight(b.toInt().coerceIn(0, 64))
        }
    }

    fun bitwiseNot64(a: BigInteger): BigInteger {
        val mask = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)
        return a.xor(mask).and(mask)
    }

    private fun groupFromRight(text: String, groupSize: Int): String {
        val negative = text.startsWith("-")
        val body = if (negative) text.substring(1) else text
        if (body.length <= groupSize) return text
        val sb = StringBuilder()
        var count = 0
        for (i in body.length - 1 downTo 0) {
            sb.append(body[i])
            count++
            if (count % groupSize == 0 && i != 0) sb.append(' ')
        }
        val grouped = sb.reverse().toString()
        return if (negative) "-$grouped" else grouped
    }
}
