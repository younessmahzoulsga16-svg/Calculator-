package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AmbientObsidianBackground
import com.example.ui.screens.BaseConverterScreen
import com.example.ui.screens.EquationSolverScreen
import com.example.ui.screens.HistoryBottomSheet
import com.example.ui.screens.ScientificCalculatorScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.CalculatorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            NovaCalcTheme {
                NovaCalcAppRoot()
            }
        }
    }
}

@Composable
fun NovaCalcAppRoot(
    viewModel: CalculatorViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val calcState by viewModel.calcState.collectAsStateWithLifecycle()
    val eqState by viewModel.equationState.collectAsStateWithLifecycle()
    val baseState by viewModel.baseState.collectAsStateWithLifecycle()

    // Support system back gesture returning from secondary modes to Scientific Calculator
    BackHandler(enabled = activeTab != AppTab.CALC) {
        viewModel.selectTab(AppTab.CALC)
    }

    AmbientObsidianBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TopGlassHeaderBar(
                    activeTab = activeTab,
                    historyCount = calcState.history.size,
                    onSelectTab = { viewModel.selectTab(it) },
                    onOpenHistory = { viewModel.setHistorySheetVisible(true) }
                )

                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInHorizontally { width ->
                            if (targetState.ordinal > initialState.ordinal) width / 8 else -width / 8
                        }).togetherWith(
                            fadeOut(tween(150))
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "app_tab_transition"
                ) { tab ->
                    when (tab) {
                        AppTab.CALC -> ScientificCalculatorScreen(
                            state = calcState,
                            viewModel = viewModel
                        )
                        AppTab.EQUATIONS -> EquationSolverScreen(
                            state = eqState,
                            viewModel = viewModel
                        )
                        AppTab.BASES -> BaseConverterScreen(
                            state = baseState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        if (calcState.showHistorySheet) {
            HistoryBottomSheet(
                history = calcState.history,
                onDismiss = { viewModel.setHistorySheetVisible(false) },
                onSelectExpression = { viewModel.loadFromHistory(it) },
                onInsertResult = { viewModel.insertResultFromHistory(it) },
                onClearHistory = { viewModel.clearHistory() }
            )
        }
    }
}

@Composable
private fun TopGlassHeaderBar(
    activeTab: AppTab,
    historyCount: Int,
    onSelectTab: (AppTab) -> Unit,
    onOpenHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Glassmorphic Segmented Mode Pill
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(Color(0x18FFFFFF))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0x38FFFFFF), Color(0x1438BDF8))
                    ),
                    shape = CircleShape
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppTab.entries.forEach { tab ->
                val selected = activeTab == tab
                val bgBrush = if (selected) {
                    Brush.horizontalGradient(
                        colors = listOf(ElectricBlue.copy(alpha = 0.78f), ElectricBlueBright.copy(alpha = 0.55f))
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(bgBrush)
                        .then(
                            if (selected) Modifier.border(
                                1.dp,
                                CyanGlow.copy(alpha = 0.65f),
                                CircleShape
                            ) else Modifier
                        )
                        .clickable { onSelectTab(tab) }
                        .testTag("tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // History Glass Icon Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x18FFFFFF))
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenHistory)
                .testTag("history_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = "Calculation History",
                tint = if (historyCount > 0) CyanGlow else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
