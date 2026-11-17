package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EquationType
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassKeyButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.KeyStyle
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.EquationSolverUiState

@Composable
fun EquationSolverScreen(
    state: EquationSolverUiState,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val solveGlow = remember { Animatable(0f) }
    LaunchedEffect(state.solvePulseId) {
        if (state.solvePulseId > 0L) {
            solveGlow.snapTo(1f)
            solveGlow.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Equation type horizontal selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EquationType.entries.forEach { eqType ->
                GlassPillChip(
                    text = eqType.title,
                    selected = state.type == eqType,
                    onClick = { viewModel.selectEquationType(eqType) },
                    testTag = "eq_type_${eqType.name.lowercase()}"
                )
            }
        }

        // Equation Input Glass Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Functions,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = state.type.badge,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Supports fractions & π",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                AnimatedContent(
                    targetState = state.type,
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInVertically { it / 6 })
                            .togetherWith(fadeOut(tween(150)))
                    },
                    label = "equation_inputs"
                ) { eqType ->
                    when (eqType) {
                        EquationType.LINEAR -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CoeffInputBox("a (x)", state.linA, { viewModel.updateLinear(a = it) }, Modifier.weight(1f), "eq_lin_a")
                                CoeffInputBox("b", state.linB, { viewModel.updateLinear(b = it) }, Modifier.weight(1f), "eq_lin_b")
                                CoeffInputBox("= c", state.linC, { viewModel.updateLinear(c = it) }, Modifier.weight(1f), "eq_lin_c")
                            }
                        }
                        EquationType.QUADRATIC -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CoeffInputBox("a (x²)", state.quadA, { viewModel.updateQuadratic(a = it) }, Modifier.weight(1f), "eq_quad_a")
                                CoeffInputBox("b (x)", state.quadB, { viewModel.updateQuadratic(b = it) }, Modifier.weight(1f), "eq_quad_b")
                                CoeffInputBox("c", state.quadC, { viewModel.updateQuadratic(c = it) }, Modifier.weight(1f), "eq_quad_c")
                            }
                        }
                        EquationType.CUBIC -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CoeffInputBox("a (x³)", state.cubA, { viewModel.updateCubic(a = it) }, Modifier.weight(1f), "eq_cub_a")
                                    CoeffInputBox("b (x²)", state.cubB, { viewModel.updateCubic(b = it) }, Modifier.weight(1f), "eq_cub_b")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CoeffInputBox("c (x)", state.cubC, { viewModel.updateCubic(c = it) }, Modifier.weight(1f), "eq_cub_c")
                                    CoeffInputBox("d", state.cubD, { viewModel.updateCubic(d = it) }, Modifier.weight(1f), "eq_cub_d")
                                }
                            }
                        }
                        EquationType.SYSTEM_2X2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CoeffInputBox("a₁x", state.sys2A1, { viewModel.updateSys2(a1 = it) }, Modifier.weight(1f), "eq_2x2_a1")
                                    CoeffInputBox("b₁y", state.sys2B1, { viewModel.updateSys2(b1 = it) }, Modifier.weight(1f), "eq_2x2_b1")
                                    CoeffInputBox("= c₁", state.sys2C1, { viewModel.updateSys2(c1 = it) }, Modifier.weight(1f), "eq_2x2_c1")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CoeffInputBox("a₂x", state.sys2A2, { viewModel.updateSys2(a2 = it) }, Modifier.weight(1f), "eq_2x2_a2")
                                    CoeffInputBox("b₂y", state.sys2B2, { viewModel.updateSys2(b2 = it) }, Modifier.weight(1f), "eq_2x2_b2")
                                    CoeffInputBox("= c₂", state.sys2C2, { viewModel.updateSys2(c2 = it) }, Modifier.weight(1f), "eq_2x2_c2")
                                }
                            }
                        }
                        EquationType.SYSTEM_3X3 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val headers = listOf("x", "y", "z", "=")
                                for (r in 0..2) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (c in 0..3) {
                                            CoeffInputBox(
                                                label = "${headers[c]}${r + 1}",
                                                value = state.sys3Matrix[r][c],
                                                onValueChange = { viewModel.updateSys3Cell(r, c, it) },
                                                modifier = Modifier.weight(1f),
                                                testTag = "eq_3x3_${r}_${c}"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        EquationType.CUSTOM_FX -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CoeffInputBox(
                                    label = "Equation with x (e.g. x^2 - 5x + 6 = 0)",
                                    value = state.customEquation,
                                    onValueChange = { viewModel.updateCustomEquation(eq = it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "eq_custom_expr",
                                    alignLeft = true
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CoeffInputBox(
                                        label = "Initial guess x₀",
                                        value = state.customGuess,
                                        onValueChange = { viewModel.updateCustomEquation(guess = it) },
                                        modifier = Modifier.weight(0.42f),
                                        testTag = "eq_custom_guess"
                                    )
                                    Row(
                                        modifier = Modifier
                                            .weight(0.58f)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("x^2 - 2 = 0", "sin(x) = 0.5", "e^x = 10").forEach { preset ->
                                            GlassPillChip(
                                                text = preset,
                                                selected = state.customEquation == preset,
                                                onClick = {
                                                    viewModel.updateCustomEquation(eq = preset)
                                                    viewModel.solveCurrentEquation()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                GlassKeyButton(
                    label = "Solve Equation",
                    onClick = { viewModel.solveCurrentEquation() },
                    testTag = "solve_equation_button",
                    style = KeyStyle.PRIMARY_EQUALS,
                    fontSize = 16.sp,
                    cornerRadius = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }

        // Animated Solution Output Card
        val sol = state.solution
        if (sol != null) {
            AnimatedContent(
                targetState = Pair(state.solvePulseId, sol),
                transitionSpec = {
                    (fadeIn(tween(250)) + slideInVertically(
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) { it / 5 }).togetherWith(fadeOut(tween(150)))
                },
                label = "equation_solution_anim"
            ) { (_, currentSol) ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("equation_solution_card"),
                    shape = RoundedCornerShape(24.dp),
                    blueTint = !currentSol.isError,
                    glowIntensity = solveGlow.value,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = if (currentSol.isError) ErrorRose else CyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = currentSol.summary,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (currentSol.isError) ErrorRose else CyanGlow
                            )
                        }

                        if (currentSol.items.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                currentSol.items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x16FFFFFF))
                                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = IceBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = item.value,
                                                fontFamily = SpaceGroteskFontFamily,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (item.alternateValue != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 2.dp)
                                                        .clip(CircleShape)
                                                        .background(ElectricBlue.copy(alpha = 0.22f))
                                                        .padding(horizontal = 8.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "= ${item.alternateValue}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = CyanGlow
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (currentSol.steps.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x18000000))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "METHOD & STEPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                currentSol.steps.forEach { step ->
                                    Text(
                                        text = "• $step",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoeffInputBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    alignLeft: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x16FFFFFF))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IceBlue,
            maxLines = 1
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(CyanGlow),
            textStyle = TextStyle(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = if (alignLeft) TextAlign.Start else TextAlign.End
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        )
    }
}
