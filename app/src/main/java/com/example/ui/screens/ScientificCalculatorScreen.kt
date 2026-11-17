package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassKeyButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.KeyStyle
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.ScientificCalcUiState

@Composable
fun ScientificCalculatorScreen(
    state: ScientificCalcUiState,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    // Trigger smooth electric-blue glow pulse on every calculation commit
    val calcGlow = remember { Animatable(0f) }
    LaunchedEffect(state.calculationPulseId) {
        if (state.calculationPulseId > 0L) {
            calcGlow.snapTo(1f)
            calcGlow.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    // Blinking cursor animation
    val cursorTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        val isCompactHeight = maxHeight < 680.dp
        val isWide = maxWidth > 620.dp

        if (isWide) {
            // Adaptive landscape / tablet split layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DisplayGlassPanel(
                        state = state,
                        calcGlow = calcGlow.value,
                        cursorAlpha = cursorAlpha,
                        viewModel = viewModel,
                        isCompact = false,
                        modifier = Modifier.weight(1f)
                    )
                    MemoryAndFormatStrip(state = state, viewModel = viewModel)
                }
                Column(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScientificKeypadMatrix(
                        state = state,
                        viewModel = viewModel,
                        isCompact = false,
                        modifier = Modifier.weight(0.38f)
                    )
                    MainNumpadMatrix(
                        viewModel = viewModel,
                        isCompact = false,
                        modifier = Modifier.weight(0.62f)
                    )
                }
            }
        } else {
            // Standard vertical phone layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 6.dp else 8.dp)
            ) {
                DisplayGlassPanel(
                    state = state,
                    calcGlow = calcGlow.value,
                    cursorAlpha = cursorAlpha,
                    viewModel = viewModel,
                    isCompact = isCompactHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (isCompactHeight) 0.28f else 0.30f)
                )

                MemoryAndFormatStrip(state = state, viewModel = viewModel)

                ScientificKeypadMatrix(
                    state = state,
                    viewModel = viewModel,
                    isCompact = isCompactHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.24f)
                )

                MainNumpadMatrix(
                    viewModel = viewModel,
                    isCompact = isCompactHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.42f)
                )
            }
        }
    }
}

@Composable
private fun DisplayGlassPanel(
    state: ScientificCalcUiState,
    calcGlow: Float,
    cursorAlpha: Float,
    viewModel: CalculatorViewModel,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val exprScroll = rememberScrollState()
    LaunchedEffect(state.expression, state.cursorPos) {
        exprScroll.animateScrollTo(exprScroll.maxValue)
    }

    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        blueTint = state.hasCommitted && state.justEvaluated,
        glowIntensity = calcGlow,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = if (isCompact) 10.dp else 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar inside display: Angle, Mode, Memory, Cursor arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassPillChip(
                        text = state.angleUnit.label,
                        selected = true,
                        onClick = { viewModel.toggleAngleUnit() },
                        testTag = "angle_unit_chip"
                    )
                    GlassPillChip(
                        text = state.displayMode.label,
                        selected = state.displayMode != com.example.engine.NumberDisplayMode.AUTO,
                        onClick = { viewModel.cycleDisplayMode() },
                        testTag = "display_mode_chip"
                    )
                    AnimatedVisibility(
                        visible = state.hasMemory,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "M",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Cursor nudge buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.moveCursorLeft() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cursor_left_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Move cursor left",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.moveCursorRight() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("cursor_right_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Move cursor right",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expression + Result column
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                // Previous committed equation line
                AnimatedVisibility(
                    visible = state.lastEquation.isNotEmpty() && state.justEvaluated,
                    enter = fadeIn(tween(200)) + slideInVertically { it / 2 },
                    exit = fadeOut(tween(150))
                ) {
                    Text(
                        text = "${state.lastEquation} =",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Current Expression with blinking cursor
                val exprFontSize = when {
                    state.expression.length > 26 -> 20.sp
                    state.expression.length > 16 -> 24.sp
                    else -> if (isCompact) 26.sp else 30.sp
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(exprScroll),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.expression.isEmpty()) {
                        Text(
                            text = "Enter equation...",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = if (isCompact) 22.sp else 26.sp,
                            color = TextMuted.copy(alpha = 0.6f),
                            textAlign = TextAlign.End
                        )
                    } else {
                        Text(
                            text = buildExpressionAnnotatedString(
                                expr = state.expression,
                                cursorPos = state.cursorPos,
                                showCursor = !state.justEvaluated,
                                cursorAlpha = cursorAlpha
                            ),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = exprFontSize,
                            color = if (state.justEvaluated) TextSecondary else TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.testTag("expression_display")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

                // Animated Result Readout
                val displayedResult = when {
                    state.errorMessage != null -> state.errorMessage
                    state.justEvaluated -> state.committedResult
                    state.expression.isEmpty() -> "0"
                    else -> state.liveResult
                }

                val resultFontSize = when {
                    state.errorMessage != null -> 20.sp
                    displayedResult.length > 18 -> 24.sp
                    displayedResult.length > 12 -> 30.sp
                    else -> if (isCompact) 36.sp else 42.sp
                }

                AnimatedContent(
                    targetState = Triple(displayedResult, state.justEvaluated, state.errorMessage != null),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) +
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { height -> height / 3 } +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(220)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(140)) +
                                    slideOutVertically { height -> -height / 4 }
                            )
                    },
                    label = "result_animation"
                ) { (resText, committed, isErr) ->
                    Text(
                        text = resText,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = if (committed) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = resultFontSize,
                        color = when {
                            isErr -> ErrorRose
                            committed -> CyanGlow
                            state.expression.isEmpty() -> TextMuted
                            else -> TextPrimary.copy(alpha = 0.85f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("result_display")
                    )
                }

                // Alternate Representation Chip (e.g. exact Fraction / Polar)
                val altText = state.liveAlternate
                AnimatedVisibility(
                    visible = altText != null && state.errorMessage == null && state.expression.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (altText != null) {
                        Row(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.18f))
                                .clickable { viewModel.toggleFractionDecimal() }
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                .testTag("alternate_result_chip"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "S⇔D",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "= $altText",
                                style = MaterialTheme.typography.labelSmall,
                                color = IceBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildExpressionAnnotatedString(
    expr: String,
    cursorPos: Int,
    showCursor: Boolean,
    cursorAlpha: Float
): AnnotatedString {
    val safePos = cursorPos.coerceIn(0, expr.length)
    return buildAnnotatedString {
        val before = expr.substring(0, safePos)
        val after = expr.substring(safePos)
        appendStyledMath(before)
        if (showCursor) {
            withStyle(
                SpanStyle(
                    color = CyanGlow.copy(alpha = cursorAlpha),
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("|")
            }
        }
        appendStyledMath(after)
    }
}

private fun AnnotatedString.Builder.appendStyledMath(text: String) {
    for (ch in text) {
        when (ch) {
            '+', '−', '×', '÷', '^', '=' -> {
                withStyle(SpanStyle(color = IceBlue, fontWeight = FontWeight.Bold)) {
                    append(ch)
                }
            }
            'π', 'e', 'φ', 'i' -> {
                withStyle(SpanStyle(color = CyanGlow)) {
                    append(ch)
                }
            }
            else -> append(ch)
        }
    }
}

@Composable
private fun MemoryAndFormatStrip(
    state: ScientificCalcUiState,
    viewModel: CalculatorViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val stripButtons = listOf(
            Triple("MC", "mem_clear") { viewModel.memoryClear() },
            Triple("MR", "mem_recall") { viewModel.memoryRecall() },
            Triple("M+", "mem_add") { viewModel.memoryAdd() },
            Triple("M−", "mem_sub") { viewModel.memorySubtract() },
            Triple("S⇔D", "sd_toggle") { viewModel.toggleFractionDecimal() },
            Triple("Ans", "key_ans") { viewModel.insertToken("Ans") },
            Triple("i", "key_imag") { viewModel.insertToken("i") }
        )
        stripButtons.forEach { (label, tag, action) ->
            GlassKeyButton(
                label = label,
                onClick = action,
                testTag = tag,
                style = if (label == "S⇔D" && state.displayMode == com.example.engine.NumberDisplayMode.FRACTION) {
                    KeyStyle.TOGGLE_ACTIVE
                } else {
                    KeyStyle.SCIENTIFIC
                },
                fontSize = 12.sp,
                cornerRadius = 12.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ScientificKeypadMatrix(
    state: ScientificCalcUiState,
    viewModel: CalculatorViewModel,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val gap = if (isCompact) 5.dp else 6.dp
    val fnFont = if (isCompact) 13.sp else 14.sp
    val corner = 14.dp

    val sinLabel = when {
        state.isHypMode && state.isSecondMode -> "sinh⁻¹"
        state.isHypMode -> "sinh"
        state.isSecondMode -> "sin⁻¹"
        else -> "sin"
    }
    val cosLabel = when {
        state.isHypMode && state.isSecondMode -> "cosh⁻¹"
        state.isHypMode -> "cosh"
        state.isSecondMode -> "cos⁻¹"
        else -> "cos"
    }
    val tanLabel = when {
        state.isHypMode && state.isSecondMode -> "tanh⁻¹"
        state.isHypMode -> "tanh"
        state.isSecondMode -> "tan⁻¹"
        else -> "tan"
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Row 1: 2nd, HYP, sin, cos, tan
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            GlassKeyButton(
                label = "2nd",
                onClick = { viewModel.toggleSecondMode() },
                testTag = "key_2nd",
                style = if (state.isSecondMode) KeyStyle.TOGGLE_ACTIVE else KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = "HYP",
                onClick = { viewModel.toggleHypMode() },
                testTag = "key_hyp",
                style = if (state.isHypMode) KeyStyle.TOGGLE_ACTIVE else KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = sinLabel,
                onClick = { viewModel.insertToken("$sinLabel(") },
                testTag = "key_sin",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = cosLabel,
                onClick = { viewModel.insertToken("$cosLabel(") },
                testTag = "key_cos",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = tanLabel,
                onClick = { viewModel.insertToken("$tanLabel(") },
                testTag = "key_tan",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 2: x²/x³, √/∛, xʸ/nCr, ln/eˣ, log/10ˣ
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            GlassKeyButton(
                label = if (state.isSecondMode) "x³" else "x²",
                onClick = { viewModel.insertToken(if (state.isSecondMode) "³" else "²", isOperator = true) },
                testTag = "key_square",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = if (state.isSecondMode) "∛" else "√",
                onClick = { viewModel.insertToken(if (state.isSecondMode) "∛(" else "√(") },
                testTag = "key_sqrt",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = if (state.isSecondMode) "nCr" else "xʸ",
                onClick = {
                    if (state.isSecondMode) viewModel.insertToken(" nCr ", isOperator = true)
                    else viewModel.insertToken("^", isOperator = true)
                },
                testTag = "key_power",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = if (state.isSecondMode) "eˣ" else "ln",
                onClick = { viewModel.insertToken(if (state.isSecondMode) "e^(" else "ln(") },
                testTag = "key_ln",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = if (state.isSecondMode) "10ˣ" else "log",
                onClick = { viewModel.insertToken(if (state.isSecondMode) "10^(" else "log(") },
                testTag = "key_log",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 3: π, e, n!/nPr, |x|/mod, %/log₂
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            GlassKeyButton(
                label = if (state.isSecondMode) "φ" else "π",
                onClick = { viewModel.insertToken(if (state.isSecondMode) "φ" else "π") },
                testTag = "key_pi",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = if (state.isSecondMode) "E" else "e",
                onClick = { viewModel.insertToken(if (state.isSecondMode) "×10^" else "e") },
                testTag = "key_euler",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = if (state.isSecondMode) "nPr" else "n!",
                onClick = {
                    if (state.isSecondMode) viewModel.insertToken(" nPr ", isOperator = true)
                    else viewModel.insertToken("!", isOperator = true)
                },
                testTag = "key_fact",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = if (state.isSecondMode) "mod" else "|x|",
                onClick = {
                    if (state.isSecondMode) viewModel.insertToken(" mod ", isOperator = true)
                    else viewModel.insertToken("abs(")
                },
                testTag = "key_abs_mod",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = if (state.isSecondMode) "log₂" else "%",
                onClick = {
                    if (state.isSecondMode) viewModel.insertToken("log₂(")
                    else viewModel.insertToken("%", isOperator = true)
                },
                testTag = "key_percent_log2",
                style = KeyStyle.SCIENTIFIC,
                fontSize = fnFont,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun MainNumpadMatrix(
    viewModel: CalculatorViewModel,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val gap = if (isCompact) 6.dp else 8.dp
    val digitSize = if (isCompact) 20.sp else 23.sp
    val opSize = if (isCompact) 22.sp else 25.sp
    val corner = 18.dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Row 1: AC, ( ), ⌫, ÷
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            GlassKeyButton(
                label = "AC",
                onClick = { viewModel.clearAll() },
                testTag = "key_ac",
                style = KeyStyle.ACTION_CLEAR,
                fontSize = 18.sp,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = "( )",
                onClick = { viewModel.insertSmartParenthesis() },
                testTag = "key_parens",
                style = KeyStyle.OPERATOR,
                fontSize = 18.sp,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = "⌫",
                onClick = { viewModel.backspace() },
                testTag = "key_backspace",
                style = KeyStyle.OPERATOR,
                fontSize = 19.sp,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = "÷",
                onClick = { viewModel.insertToken("÷", isOperator = true) },
                testTag = "key_div",
                style = KeyStyle.OPERATOR,
                fontSize = opSize,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            listOf("7", "8", "9").forEach { d ->
                GlassKeyButton(
                    label = d,
                    onClick = { viewModel.insertToken(d) },
                    testTag = "key_$d",
                    style = KeyStyle.DIGIT,
                    fontSize = digitSize,
                    cornerRadius = corner,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            GlassKeyButton(
                label = "×",
                onClick = { viewModel.insertToken("×", isOperator = true) },
                testTag = "key_mul",
                style = KeyStyle.OPERATOR,
                fontSize = opSize,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 3: 4, 5, 6, −
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            listOf("4", "5", "6").forEach { d ->
                GlassKeyButton(
                    label = d,
                    onClick = { viewModel.insertToken(d) },
                    testTag = "key_$d",
                    style = KeyStyle.DIGIT,
                    fontSize = digitSize,
                    cornerRadius = corner,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            GlassKeyButton(
                label = "−",
                onClick = { viewModel.insertToken("−", isOperator = true) },
                testTag = "key_sub",
                style = KeyStyle.OPERATOR,
                fontSize = opSize,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            listOf("1", "2", "3").forEach { d ->
                GlassKeyButton(
                    label = d,
                    onClick = { viewModel.insertToken(d) },
                    testTag = "key_$d",
                    style = KeyStyle.DIGIT,
                    fontSize = digitSize,
                    cornerRadius = corner,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            GlassKeyButton(
                label = "+",
                onClick = { viewModel.insertToken("+", isOperator = true) },
                testTag = "key_add",
                style = KeyStyle.OPERATOR,
                fontSize = opSize,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 5: +/-, 0, ., =
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            GlassKeyButton(
                label = "+/−",
                onClick = { viewModel.toggleNegate() },
                testTag = "key_negate",
                style = KeyStyle.DIGIT,
                fontSize = 18.sp,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = "0",
                onClick = { viewModel.insertToken("0") },
                testTag = "key_0",
                style = KeyStyle.DIGIT,
                fontSize = digitSize,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = ".",
                onClick = { viewModel.insertToken(".") },
                testTag = "key_dot",
                style = KeyStyle.DIGIT,
                fontSize = digitSize,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlassKeyButton(
                label = "=",
                onClick = { viewModel.evaluateFinal() },
                testTag = "key_equals",
                style = KeyStyle.PRIMARY_EQUALS,
                fontSize = 26.sp,
                cornerRadius = corner,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}
