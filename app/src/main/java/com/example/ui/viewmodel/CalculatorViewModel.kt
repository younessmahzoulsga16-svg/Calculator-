package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalcPreferencesRepository
import com.example.data.CalculationRecord
import com.example.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigInteger

enum class AppTab(val title: String) {
    CALC("Scientific"),
    EQUATIONS("Equations"),
    BASES("Bases")
}

data class ScientificCalcUiState(
    val expression: String = "",
    val cursorPos: Int = 0,
    val liveResult: String = "0",
    val liveAlternate: String? = null,
    val isLiveError: Boolean = false,
    val lastEquation: String = "",
    val committedResult: String = "0",
    val hasCommitted: Boolean = false,
    val justEvaluated: Boolean = false,
    val errorMessage: String? = null,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val displayMode: NumberDisplayMode = NumberDisplayMode.AUTO,
    val isSecondMode: Boolean = false,
    val isHypMode: Boolean = false,
    val ansValue: Complex = Complex.ZERO,
    val memoryValue: Complex = Complex.ZERO,
    val hasMemory: Boolean = false,
    val calculationPulseId: Long = 0L,
    val history: List<CalculationRecord> = emptyList(),
    val showHistorySheet: Boolean = false
)

data class EquationSolverUiState(
    val type: EquationType = EquationType.QUADRATIC,
    val linA: String = "2",
    val linB: String = "-5",
    val linC: String = "9",
    val quadA: String = "1",
    val quadB: String = "-5",
    val quadC: String = "6",
    val cubA: String = "1",
    val cubB: String = "-6",
    val cubC: String = "11",
    val cubD: String = "-6",
    val sys2A1: String = "2",
    val sys2B1: String = "3",
    val sys2C1: String = "13",
    val sys2A2: String = "5",
    val sys2B2: String = "-1",
    val sys2C2: String = "7",
    val sys3Matrix: List<List<String>> = listOf(
        listOf("2", "1", "-1", "8"),
        listOf("-3", "-1", "2", "-11"),
        listOf("-2", "1", "2", "-3")
    ),
    val customEquation: String = "x^2 - 2 = 0",
    val customGuess: String = "1.5",
    val solution: EquationSolution? = null,
    val solvePulseId: Long = 0L
)

data class BaseConverterUiState(
    val activeBase: NumberBase = NumberBase.DEC,
    val rawInput: String = "42",
    val currentValue: BigInteger = BigInteger.valueOf(42),
    val storedOperand: BigInteger? = null,
    val pendingOp: BitwiseOp? = null,
    val pulseId: Long = 0L
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalcPreferencesRepository(application.applicationContext)

    private val _activeTab = MutableStateFlow(AppTab.CALC)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _calcState = MutableStateFlow(ScientificCalcUiState())
    val calcState: StateFlow<ScientificCalcUiState> = _calcState.asStateFlow()

    private val _equationState = MutableStateFlow(EquationSolverUiState())
    val equationState: StateFlow<EquationSolverUiState> = _equationState.asStateFlow()

    private val _baseState = MutableStateFlow(BaseConverterUiState())
    val baseState: StateFlow<BaseConverterUiState> = _baseState.asStateFlow()

    private var lastValidComplex: Complex = Complex.ZERO

    init {
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                _calcState.update { state ->
                    state.copy(
                        angleUnit = prefs.angleUnit,
                        displayMode = prefs.displayMode,
                        history = prefs.history
                    )
                }
                recomputeLivePreview()
            }
        }
        solveCurrentEquation()
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    // --- Scientific Calculator Actions ---

    fun insertToken(token: String, isOperator: Boolean = false) {
        _calcState.update { state ->
            var expr = state.expression
            var cursor = state.cursorPos.coerceIn(0, expr.length)

            if (state.justEvaluated) {
                if (isOperator) {
                    expr = "Ans"
                    cursor = expr.length
                } else {
                    expr = ""
                    cursor = 0
                }
            }

            // Prevent double consecutive binary operators like ++ or ×÷ (allow minus for negative)
            val binaryOps = setOf("+", "×", "÷", "^", " mod ", " nCr ", " nPr ")
            if (token in binaryOps && cursor > 0) {
                val prevChar = expr[cursor - 1].toString()
                if (prevChar in setOf("+", "×", "÷", "^") && token.length == 1) {
                    expr = expr.removeRange(cursor - 1, cursor)
                    cursor--
                }
            }

            val updated = StringBuilder(expr).insert(cursor, token).toString()
            state.copy(
                expression = updated,
                cursorPos = cursor + token.length,
                justEvaluated = false,
                errorMessage = null
            )
        }
        recomputeLivePreview()
    }

    fun insertSmartParenthesis() {
        _calcState.update { state ->
            val expr = if (state.justEvaluated) "" else state.expression
            val cursor = if (state.justEvaluated) 0 else state.cursorPos.coerceIn(0, expr.length)
            val before = expr.substring(0, cursor)
            val openCount = before.count { it == '(' }
            val closeCount = before.count { it == ')' }
            val prevChar = before.lastOrNull()
            val shouldClose = openCount > closeCount && prevChar != null &&
                (prevChar.isDigit() || prevChar == ')' || prevChar == 'π' || prevChar == 'e' || prevChar == 'φ' || prevChar == 'i' || prevChar == '!' || prevChar == '%' || prevChar == '²' || prevChar == '³' || prevChar == 's')
            val paren = if (shouldClose) ")" else "("
            val updated = StringBuilder(expr).insert(cursor, paren).toString()
            state.copy(
                expression = updated,
                cursorPos = cursor + 1,
                justEvaluated = false,
                errorMessage = null
            )
        }
        recomputeLivePreview()
    }

    fun toggleNegate() {
        _calcState.update { state ->
            if (state.expression.isEmpty()) {
                return@update state.copy(expression = "−", cursorPos = 1, justEvaluated = false)
            }
            if (state.justEvaluated) {
                val updated = "−(${state.committedResult})"
                return@update state.copy(
                    expression = updated,
                    cursorPos = updated.length,
                    justEvaluated = false,
                    errorMessage = null
                )
            }
            val expr = state.expression
            val updated = if (expr.startsWith("−(") && expr.endsWith(")")) {
                expr.substring(2, expr.length - 1)
            } else if (expr.startsWith("−")) {
                expr.substring(1)
            } else {
                "−($expr)"
            }
            state.copy(
                expression = updated,
                cursorPos = updated.length,
                justEvaluated = false,
                errorMessage = null
            )
        }
        recomputeLivePreview()
    }

    fun backspace() {
        _calcState.update { state ->
            if (state.justEvaluated) {
                return@update state.copy(
                    expression = "",
                    cursorPos = 0,
                    justEvaluated = false,
                    errorMessage = null
                )
            }
            val expr = state.expression
            val cursor = state.cursorPos.coerceIn(0, expr.length)
            if (cursor == 0 || expr.isEmpty()) return@update state

            val multiTokens = listOf(
                "sin⁻¹(", "cos⁻¹(", "tan⁻¹(",
                "sinh⁻¹(", "cosh⁻¹(", "tanh⁻¹(",
                "sinh(", "cosh(", "tanh(",
                "sin(", "cos(", "tan(",
                "log₂(", "log(", "ln(",
                "√(", "∛(", "abs(", "exp(",
                " nCr ", " nPr ", " mod ", "Ans"
            )
            val prefix = expr.substring(0, cursor)
            val matchedToken = multiTokens.firstOrNull { prefix.endsWith(it) }
            val deleteCount = matchedToken?.length ?: 1
            val newCursor = (cursor - deleteCount).coerceAtLeast(0)
            val updated = expr.removeRange(newCursor, cursor)

            state.copy(
                expression = updated,
                cursorPos = newCursor,
                errorMessage = null
            )
        }
        recomputeLivePreview()
    }

    fun clearAll() {
        _calcState.update { state ->
            state.copy(
                expression = "",
                cursorPos = 0,
                liveResult = "0",
                liveAlternate = null,
                isLiveError = false,
                lastEquation = "",
                committedResult = "0",
                hasCommitted = false,
                justEvaluated = false,
                errorMessage = null
            )
        }
    }

    fun moveCursorLeft() {
        _calcState.update { state ->
            state.copy(
                cursorPos = (state.cursorPos - 1).coerceAtLeast(0),
                justEvaluated = false
            )
        }
    }

    fun moveCursorRight() {
        _calcState.update { state ->
            state.copy(
                cursorPos = (state.cursorPos + 1).coerceAtMost(state.expression.length),
                justEvaluated = false
            )
        }
    }

    fun evaluateFinal() {
        val state = _calcState.value
        val rawExpr = state.expression.trim()
        if (rawExpr.isEmpty()) return

        val normalizedExpr = normalizeTokensForEngine(rawExpr)
        try {
            val value = MathExpressionEngine.evaluate(
                expression = normalizedExpr,
                angleUnit = state.angleUnit,
                ans = state.ansValue
            )
            lastValidComplex = value
            val formatted = NumberFormatter.format(value, state.displayMode, state.angleUnit)
            val alt = NumberFormatter.getAlternateRepresentation(value, state.displayMode, state.angleUnit)

            val newRecord = CalculationRecord(
                id = System.currentTimeMillis(),
                expression = rawExpr,
                result = formatted,
                alternateResult = alt,
                angleUnit = state.angleUnit,
                timestampMs = System.currentTimeMillis()
            )
            val updatedHistory = listOf(newRecord) + state.history.take(59)

            _calcState.update {
                it.copy(
                    lastEquation = rawExpr,
                    committedResult = formatted,
                    liveResult = formatted,
                    liveAlternate = alt,
                    isLiveError = false,
                    hasCommitted = true,
                    justEvaluated = true,
                    errorMessage = null,
                    ansValue = value,
                    calculationPulseId = it.calculationPulseId + 1L,
                    history = updatedHistory
                )
            }
            viewModelScope.launch {
                repository.saveHistory(updatedHistory)
            }
        } catch (e: Exception) {
            _calcState.update {
                it.copy(
                    errorMessage = e.message ?: "Syntax Error",
                    isLiveError = true,
                    calculationPulseId = it.calculationPulseId + 1L
                )
            }
        }
    }

    fun toggleAngleUnit() {
        val next = when (_calcState.value.angleUnit) {
            AngleUnit.DEG -> AngleUnit.RAD
            AngleUnit.RAD -> AngleUnit.GRAD
            AngleUnit.GRAD -> AngleUnit.DEG
        }
        _calcState.update { it.copy(angleUnit = next) }
        viewModelScope.launch { repository.saveAngleUnit(next) }
        recomputeLivePreview()
    }

    fun cycleDisplayMode() {
        val current = _calcState.value.displayMode
        val next = when (current) {
            NumberDisplayMode.AUTO -> NumberDisplayMode.FRACTION
            NumberDisplayMode.FRACTION -> NumberDisplayMode.SCIENTIFIC
            NumberDisplayMode.SCIENTIFIC -> if (!lastValidComplex.isReal) NumberDisplayMode.POLAR else NumberDisplayMode.AUTO
            NumberDisplayMode.POLAR -> NumberDisplayMode.AUTO
        }
        _calcState.update { it.copy(displayMode = next, calculationPulseId = it.calculationPulseId + 1L) }
        viewModelScope.launch { repository.saveDisplayMode(next) }
        refreshFormattedDisplays()
    }

    fun toggleFractionDecimal() {
        val current = _calcState.value.displayMode
        val next = if (current == NumberDisplayMode.FRACTION) NumberDisplayMode.AUTO else NumberDisplayMode.FRACTION
        _calcState.update { it.copy(displayMode = next, calculationPulseId = it.calculationPulseId + 1L) }
        viewModelScope.launch { repository.saveDisplayMode(next) }
        refreshFormattedDisplays()
    }

    fun toggleSecondMode() {
        _calcState.update { it.copy(isSecondMode = !it.isSecondMode) }
    }

    fun toggleHypMode() {
        _calcState.update { it.copy(isHypMode = !it.isHypMode) }
    }

    fun memoryClear() {
        _calcState.update { it.copy(memoryValue = Complex.ZERO, hasMemory = false) }
    }

    fun memoryRecall() {
        insertToken(NumberFormatter.formatRealCompact(_calcState.value.memoryValue.re))
    }

    fun memoryAdd() {
        val currentVal = getCurrentDisplayComplex() ?: return
        _calcState.update {
            val next = it.memoryValue + currentVal
            it.copy(memoryValue = next, hasMemory = !next.isZero)
        }
    }

    fun memorySubtract() {
        val currentVal = getCurrentDisplayComplex() ?: return
        _calcState.update {
            val next = it.memoryValue - currentVal
            it.copy(memoryValue = next, hasMemory = !next.isZero)
        }
    }

    fun setHistorySheetVisible(visible: Boolean) {
        _calcState.update { it.copy(showHistorySheet = visible) }
    }

    fun loadFromHistory(record: CalculationRecord) {
        _calcState.update {
            it.copy(
                expression = record.expression,
                cursorPos = record.expression.length,
                justEvaluated = false,
                showHistorySheet = false,
                errorMessage = null
            )
        }
        recomputeLivePreview()
    }

    fun insertResultFromHistory(record: CalculationRecord) {
        _calcState.update { it.copy(showHistorySheet = false) }
        insertToken(record.result.replace(",", ""))
    }

    fun clearHistory() {
        _calcState.update { it.copy(history = emptyList()) }
        viewModelScope.launch { repository.saveHistory(emptyList()) }
    }

    private fun getCurrentDisplayComplex(): Complex? {
        val state = _calcState.value
        if (state.expression.isBlank()) return state.ansValue
        return try {
            MathExpressionEngine.evaluate(
                normalizeTokensForEngine(state.expression),
                state.angleUnit,
                state.ansValue
            )
        } catch (_: Exception) {
            if (state.hasCommitted) state.ansValue else null
        }
    }

    private fun refreshFormattedDisplays() {
        val state = _calcState.value
        try {
            val formatted = NumberFormatter.format(lastValidComplex, state.displayMode, state.angleUnit)
            val alt = NumberFormatter.getAlternateRepresentation(lastValidComplex, state.displayMode, state.angleUnit)
            _calcState.update {
                it.copy(
                    liveResult = formatted,
                    committedResult = if (it.hasCommitted) formatted else it.committedResult,
                    liveAlternate = alt
                )
            }
        } catch (_: Exception) { }
    }

    private fun recomputeLivePreview() {
        val state = _calcState.value
        val rawExpr = state.expression.trim()
        if (rawExpr.isEmpty()) {
            _calcState.update { it.copy(liveResult = "0", liveAlternate = null, isLiveError = false) }
            return
        }
        try {
            val value = MathExpressionEngine.evaluate(
                expression = normalizeTokensForEngine(rawExpr),
                angleUnit = state.angleUnit,
                ans = state.ansValue
            )
            lastValidComplex = value
            val formatted = NumberFormatter.format(value, state.displayMode, state.angleUnit)
            val alt = NumberFormatter.getAlternateRepresentation(value, state.displayMode, state.angleUnit)
            _calcState.update {
                it.copy(
                    liveResult = formatted,
                    liveAlternate = alt,
                    isLiveError = false,
                    committedResult = if (it.justEvaluated) formatted else it.committedResult
                )
            }
        } catch (_: Exception) {
            _calcState.update { it.copy(isLiveError = true) }
        }
    }

    private fun normalizeTokensForEngine(expr: String): String {
        return expr
            .replace("log₂(", "log2(")
            .replace("sinh⁻¹(", "asinh(")
            .replace("cosh⁻¹(", "acosh(")
            .replace("tanh⁻¹(", "atanh(")
    }

    // --- Equation Solver Actions ---

    fun selectEquationType(type: EquationType) {
        _equationState.update { it.copy(type = type) }
        solveCurrentEquation()
    }

    fun updateLinear(a: String? = null, b: String? = null, c: String? = null) {
        _equationState.update { s ->
            s.copy(linA = a ?: s.linA, linB = b ?: s.linB, linC = c ?: s.linC)
        }
    }

    fun updateQuadratic(a: String? = null, b: String? = null, c: String? = null) {
        _equationState.update { s ->
            s.copy(quadA = a ?: s.quadA, quadB = b ?: s.quadB, quadC = c ?: s.quadC)
        }
    }

    fun updateCubic(a: String? = null, b: String? = null, c: String? = null, d: String? = null) {
        _equationState.update { s ->
            s.copy(cubA = a ?: s.cubA, cubB = b ?: s.cubB, cubC = c ?: s.cubC, cubD = d ?: s.cubD)
        }
    }

    fun updateSys2(
        a1: String? = null, b1: String? = null, c1: String? = null,
        a2: String? = null, b2: String? = null, c2: String? = null
    ) {
        _equationState.update { s ->
            s.copy(
                sys2A1 = a1 ?: s.sys2A1, sys2B1 = b1 ?: s.sys2B1, sys2C1 = c1 ?: s.sys2C1,
                sys2A2 = a2 ?: s.sys2A2, sys2B2 = b2 ?: s.sys2B2, sys2C2 = c2 ?: s.sys2C2
            )
        }
    }

    fun updateSys3Cell(row: Int, col: Int, value: String) {
        _equationState.update { s ->
            val updated = s.sys3Matrix.mapIndexed { r, rowList ->
                if (r == row) rowList.mapIndexed { c, old -> if (c == col) value else old }
                else rowList
            }
            s.copy(sys3Matrix = updated)
        }
    }

    fun updateCustomEquation(eq: String? = null, guess: String? = null) {
        _equationState.update { s ->
            s.copy(customEquation = eq ?: s.customEquation, customGuess = guess ?: s.customGuess)
        }
    }

    fun solveCurrentEquation() {
        val eq = _equationState.value
        val angleUnit = _calcState.value.angleUnit
        fun parseCoeff(text: String): Double {
            val cleaned = text.trim()
            if (cleaned.isEmpty() || cleaned == "-") return 0.0
            val res = MathExpressionEngine.evaluate(cleaned, angleUnit)
            if (!res.isReal) throw MathEvalException("Coefficients must be real numbers")
            return res.re
        }

        val solution = try {
            when (eq.type) {
                EquationType.LINEAR -> EquationSolver.solveLinear(
                    parseCoeff(eq.linA), parseCoeff(eq.linB), parseCoeff(eq.linC)
                )
                EquationType.QUADRATIC -> EquationSolver.solveQuadratic(
                    parseCoeff(eq.quadA), parseCoeff(eq.quadB), parseCoeff(eq.quadC)
                )
                EquationType.CUBIC -> EquationSolver.solveCubic(
                    parseCoeff(eq.cubA), parseCoeff(eq.cubB), parseCoeff(eq.cubC), parseCoeff(eq.cubD)
                )
                EquationType.SYSTEM_2X2 -> EquationSolver.solve2x2(
                    parseCoeff(eq.sys2A1), parseCoeff(eq.sys2B1), parseCoeff(eq.sys2C1),
                    parseCoeff(eq.sys2A2), parseCoeff(eq.sys2B2), parseCoeff(eq.sys2C2)
                )
                EquationType.SYSTEM_3X3 -> {
                    val m = Array(3) { r -> DoubleArray(3) { c -> parseCoeff(eq.sys3Matrix[r][c]) } }
                    val rhs = DoubleArray(3) { r -> parseCoeff(eq.sys3Matrix[r][3]) }
                    EquationSolver.solve3x3(m, rhs)
                }
                EquationType.CUSTOM_FX -> EquationSolver.solveCustomEquation(
                    eq.customEquation, parseCoeff(eq.customGuess), angleUnit
                )
            }
        } catch (e: Exception) {
            EquationSolution(
                summary = e.message ?: "Invalid input coefficient",
                items = emptyList(),
                steps = listOf("You can enter integers, decimals, fractions (e.g. 1/2), or constants (e.g. pi)."),
                isError = true
            )
        }

        _equationState.update {
            it.copy(solution = solution, solvePulseId = it.solvePulseId + 1L)
        }
    }

    // --- Number Base & Bitwise Actions ---

    fun selectBase(base: NumberBase) {
        _baseState.update { state ->
            val raw = state.currentValue.toString(base.radix).uppercase()
            state.copy(activeBase = base, rawInput = raw, pulseId = state.pulseId + 1L)
        }
    }

    fun appendBaseDigit(digit: String) {
        _baseState.update { state ->
            val base = state.activeBase
            val validChars = when (base) {
                NumberBase.BIN -> setOf("0", "1")
                NumberBase.OCT -> ('0'..'7').map { it.toString() }.toSet()
                NumberBase.DEC -> ('0'..'9').map { it.toString() }.toSet()
                NumberBase.HEX -> (('0'..'9') + ('A'..'F')).map { it.toString() }.toSet()
            }
            if (digit !in validChars) return@update state
            val newRaw = if (state.rawInput == "0") digit else state.rawInput + digit
            val parsed = NumberBaseConverter.parse(newRaw, base) ?: return@update state
            if (newRaw.length > 64) return@update state
            state.copy(rawInput = newRaw, currentValue = parsed, pulseId = state.pulseId + 1L)
        }
    }

    fun backspaceBase() {
        _baseState.update { state ->
            val nextRaw = if (state.rawInput.length <= 1) "0" else state.rawInput.dropLast(1)
            val parsed = NumberBaseConverter.parse(nextRaw, state.activeBase) ?: BigInteger.ZERO
            state.copy(rawInput = nextRaw, currentValue = parsed, pulseId = state.pulseId + 1L)
        }
    }

    fun clearBase() {
        _baseState.update {
            BaseConverterUiState(activeBase = it.activeBase, rawInput = "0", currentValue = BigInteger.ZERO, pulseId = it.pulseId + 1L)
        }
    }

    fun applyBitwiseOperator(op: BitwiseOp) {
        _baseState.update { state ->
            val combined = if (state.storedOperand != null && state.pendingOp != null) {
                NumberBaseConverter.applyBitwise(state.storedOperand, state.currentValue, state.pendingOp)
            } else {
                state.currentValue
            }
            state.copy(
                storedOperand = combined,
                pendingOp = op,
                rawInput = "0",
                currentValue = BigInteger.ZERO,
                pulseId = state.pulseId + 1L
            )
        }
    }

    fun evaluateBitwise() {
        _baseState.update { state ->
            val stored = state.storedOperand ?: return@update state
            val op = state.pendingOp ?: return@update state
            val res = NumberBaseConverter.applyBitwise(stored, state.currentValue, op)
            state.copy(
                currentValue = res,
                rawInput = res.toString(state.activeBase.radix).uppercase(),
                storedOperand = null,
                pendingOp = null,
                pulseId = state.pulseId + 1L
            )
        }
    }

    fun applyBitwiseNot() {
        _baseState.update { state ->
            val res = NumberBaseConverter.bitwiseNot64(state.currentValue)
            state.copy(
                currentValue = res,
                rawInput = res.toString(state.activeBase.radix).uppercase(),
                pulseId = state.pulseId + 1L
            )
        }
    }
}
