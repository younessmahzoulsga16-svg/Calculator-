package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BitwiseOp
import com.example.engine.NumberBase
import com.example.engine.NumberBaseConverter
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassKeyButton
import com.example.ui.components.KeyStyle
import com.example.ui.theme.*
import com.example.ui.viewmodel.BaseConverterUiState
import com.example.ui.viewmodel.CalculatorViewModel

@Composable
fun BaseConverterScreen(
    state: BaseConverterUiState,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val pulseGlow = remember { Animatable(0f) }
    LaunchedEffect(state.pulseId) {
        if (state.pulseId > 0L) {
            pulseGlow.snapTo(0.85f)
            pulseGlow.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Multi-Base Live Readout Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.36f),
            shape = RoundedCornerShape(24.dp),
            blueTint = true,
            glowIntensity = pulseGlow.value,
            contentPadding = PaddingValues(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                if (state.pendingOp != null && state.storedOperand != null) {
                    Text(
                        text = "${NumberBaseConverter.format(state.storedOperand, state.activeBase)} ${state.pendingOp.symbol} ...",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanGlow,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                NumberBase.entries.forEach { base ->
                    val isSelected = state.activeBase == base
                    val formatted = NumberBaseConverter.format(state.currentValue, base)
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) ElectricBlue.copy(alpha = 0.28f) else Color.Transparent,
                        label = "base_row_bg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) CyanGlow.copy(alpha = 0.75f) else Color.Transparent,
                        label = "base_row_border"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable { viewModel.selectBase(base) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("base_row_${base.label.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) CyanGlow.copy(alpha = 0.22f) else Color(0x18FFFFFF)
                                )
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = base.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) CyanGlow else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AnimatedContent(
                            targetState = formatted,
                            transitionSpec = { fadeIn(tween(150)).togetherWith(fadeOut(tween(100))) },
                            modifier = Modifier.weight(1f),
                            label = "base_val_${base.name}"
                        ) { valStr ->
                            Text(
                                text = valStr,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = if (base == NumberBase.BIN && valStr.length > 20) 13.sp else 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Bitwise Operations Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BitwiseOp.entries.forEach { op ->
                GlassKeyButton(
                    label = op.symbol,
                    onClick = { viewModel.applyBitwiseOperator(op) },
                    style = if (state.pendingOp == op) KeyStyle.TOGGLE_ACTIVE else KeyStyle.SCIENTIFIC,
                    fontSize = 13.sp,
                    cornerRadius = 12.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            GlassKeyButton(
                label = "NOT",
                onClick = { viewModel.applyBitwiseNot() },
                style = KeyStyle.SCIENTIFIC,
                fontSize = 13.sp,
                cornerRadius = 12.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Programmer Hex/Bin Keypad
        val activeBase = state.activeBase
        fun isDigitAllowed(d: String): Boolean {
            return when (activeBase) {
                NumberBase.BIN -> d == "0" || d == "1"
                NumberBase.OCT -> d in "0".."7"
                NumberBase.DEC -> d in "0".."9"
                NumberBase.HEX -> true
            }
        }

        val rows = listOf(
            listOf("A", "B", "C", "CLR"),
            listOf("D", "E", "F", "⌫"),
            listOf("7", "8", "9", "<<"),
            listOf("4", "5", "6", ">>"),
            listOf("1", "2", "3", "0"),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.56f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { key ->
                        when (key) {
                            "CLR" -> GlassKeyButton(
                                label = "CLR",
                                onClick = { viewModel.clearBase() },
                                testTag = "base_clear",
                                style = KeyStyle.ACTION_CLEAR,
                                fontSize = 16.sp,
                                cornerRadius = 16.dp,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            "⌫" -> GlassKeyButton(
                                label = "⌫",
                                onClick = { viewModel.backspaceBase() },
                                testTag = "base_backspace",
                                style = KeyStyle.OPERATOR,
                                fontSize = 18.sp,
                                cornerRadius = 16.dp,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            "<<" -> GlassKeyButton(
                                label = "LSH",
                                onClick = { viewModel.applyBitwiseOperator(BitwiseOp.SHL) },
                                style = KeyStyle.OPERATOR,
                                fontSize = 15.sp,
                                cornerRadius = 16.dp,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            ">>" -> GlassKeyButton(
                                label = "RSH",
                                onClick = { viewModel.applyBitwiseOperator(BitwiseOp.SHR) },
                                style = KeyStyle.OPERATOR,
                                fontSize = 15.sp,
                                cornerRadius = 16.dp,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            else -> {
                                val allowed = isDigitAllowed(key)
                                GlassKeyButton(
                                    label = key,
                                    onClick = { viewModel.appendBaseDigit(key) },
                                    testTag = "base_key_$key",
                                    style = if (key in "A".."F") KeyStyle.SCIENTIFIC else KeyStyle.DIGIT,
                                    fontSize = 19.sp,
                                    cornerRadius = 16.dp,
                                    enabled = allowed,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }

            GlassKeyButton(
                label = "=",
                onClick = { viewModel.evaluateBitwise() },
                testTag = "base_equals",
                style = KeyStyle.PRIMARY_EQUALS,
                fontSize = 22.sp,
                cornerRadius = 16.dp,
                enabled = state.pendingOp != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}
