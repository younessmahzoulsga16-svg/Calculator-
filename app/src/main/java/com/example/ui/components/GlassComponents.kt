package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Deep obsidian black background with slow-breathing electric blue & cyan radial luminescence
 * that gives glassmorphic panels real refractive depth.
 */
@Composable
fun AmbientObsidianBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val drift1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_drift_1"
    )
    val drift2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_drift_2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .drawBehind {
                // Top-left sapphire ambient sphere
                val center1 = Offset(
                    x = size.width * (0.14f + 0.08f * drift1),
                    y = size.height * (0.14f + 0.05f * drift2)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = 0.24f),
                            MidnightBlue.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center1,
                        radius = size.width * 0.72f
                    ),
                    center = center1,
                    radius = size.width * 0.72f
                )

                // Mid-right cyan-blue accent sphere
                val center2 = Offset(
                    x = size.width * (0.88f - 0.10f * drift2),
                    y = size.height * (0.38f + 0.07f * drift1)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyanGlow.copy(alpha = 0.14f),
                            ElectricBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center2,
                        radius = size.width * 0.60f
                    ),
                    center = center2,
                    radius = size.width * 0.60f
                )

                // Bottom-center subtle deep blue reflection
                val center3 = Offset(
                    x = size.width * (0.45f + 0.12f * drift1),
                    y = size.height * 0.90f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricBlueBright.copy(alpha = 0.13f),
                            Color.Transparent
                        ),
                        center = center3,
                        radius = size.width * 0.65f
                    ),
                    center = center3,
                    radius = size.width * 0.65f
                )
            },
        content = content
    )
}

/**
 * Frosted dark glassmorphic surface with specular top-left border highlight
 * and optional electric blue calculation glow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blueTint: Boolean = false,
    glowIntensity: Float = 0f,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val topFill = if (blueTint) Color(0x282563EB) else Color(0x1EFFFFFF)
    val bottomFill = if (blueTint) Color(0x100D1B38) else Color(0x0B0F172A)

    val borderTopLeft = if (glowIntensity > 0.05f) {
        CyanGlow.copy(alpha = (0.35f + 0.55f * glowIntensity).coerceAtMost(1f))
    } else if (blueTint) {
        IceBlue.copy(alpha = 0.42f)
    } else {
        Color(0x42FFFFFF)
    }

    val borderBottomRight = if (glowIntensity > 0.05f) {
        ElectricBlueBright.copy(alpha = 0.45f * glowIntensity)
    } else {
        Color(0x1238BDF8)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topFill, bottomFill)
                )
            )
            .drawBehind {
                if (glowIntensity > 0.01f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyanGlow.copy(alpha = 0.16f * glowIntensity),
                                ElectricBlue.copy(alpha = 0.06f * glowIntensity),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.8f, size.height * 0.75f),
                            radius = size.maxDimension * 0.85f
                        )
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(borderTopLeft, Color(0x16FFFFFF), borderBottomRight)
                ),
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}

enum class KeyStyle {
    DIGIT,
    OPERATOR,
    SCIENTIFIC,
    ACTION_CLEAR,
    PRIMARY_EQUALS,
    TOGGLE_ACTIVE
}

@Composable
fun GlassKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    style: KeyStyle = KeyStyle.DIGIT,
    fontSize: TextUnit = 20.sp,
    cornerRadius: Dp = 18.dp,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "key_scale"
    )

    val pressGlow by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 60 else 220),
        label = "key_glow"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> TextMuted.copy(alpha = 0.4f)
            style == KeyStyle.PRIMARY_EQUALS -> Color.White
            style == KeyStyle.TOGGLE_ACTIVE -> CyanGlow
            style == KeyStyle.OPERATOR -> IceBlue
            style == KeyStyle.ACTION_CLEAR -> ErrorRose
            style == KeyStyle.SCIENTIFIC -> Color(0xFFCBD5E1)
            else -> TextPrimary
        },
        label = "key_text_color"
    )

    val shape = RoundedCornerShape(cornerRadius)

    val bgBrush = when (style) {
        KeyStyle.PRIMARY_EQUALS -> Brush.linearGradient(
            colors = listOf(
                ElectricBlueBright,
                ElectricBlue,
                Color(0xFF1D4ED8)
            )
        )
        KeyStyle.TOGGLE_ACTIVE -> Brush.verticalGradient(
            colors = listOf(
                ElectricBlue.copy(alpha = 0.40f),
                CyanGlow.copy(alpha = 0.18f)
            )
        )
        KeyStyle.OPERATOR -> Brush.verticalGradient(
            colors = listOf(
                ElectricBlue.copy(alpha = 0.24f + 0.16f * pressGlow),
                MidnightBlue.copy(alpha = 0.30f)
            )
        )
        KeyStyle.ACTION_CLEAR -> Brush.verticalGradient(
            colors = listOf(
                Color(0x28F43F5E),
                Color(0x14881337)
            )
        )
        KeyStyle.SCIENTIFIC -> Brush.verticalGradient(
            colors = listOf(
                Color(0x14FFFFFF),
                Color(0x0A0F172A)
            )
        )
        KeyStyle.DIGIT -> Brush.verticalGradient(
            colors = listOf(
                Color(0x20FFFFFF),
                Color(0x0E1E293B)
            )
        )
    }

    val borderBrush = when (style) {
        KeyStyle.PRIMARY_EQUALS -> Brush.linearGradient(
            colors = listOf(
                CyanGlow.copy(alpha = 0.90f),
                ElectricBlueBright.copy(alpha = 0.60f)
            )
        )
        KeyStyle.TOGGLE_ACTIVE -> Brush.linearGradient(
            colors = listOf(
                CyanGlow.copy(alpha = 0.85f),
                ElectricBlue.copy(alpha = 0.40f)
            )
        )
        KeyStyle.OPERATOR -> Brush.linearGradient(
            colors = listOf(
                IceBlue.copy(alpha = 0.35f + 0.45f * pressGlow),
                ElectricBlue.copy(alpha = 0.15f)
            )
        )
        KeyStyle.ACTION_CLEAR -> Brush.linearGradient(
            colors = listOf(
                ErrorRose.copy(alpha = 0.45f),
                ErrorRose.copy(alpha = 0.12f)
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.24f + 0.30f * pressGlow),
                CyanGlow.copy(alpha = 0.08f + 0.35f * pressGlow)
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    color = if (style == KeyStyle.PRIMARY_EQUALS) Color.White else CyanGlow
                ),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = fontSize,
            fontFamily = if (style == KeyStyle.DIGIT || style == KeyStyle.PRIMARY_EQUALS) {
                SpaceGroteskFontFamily
            } else {
                PlusJakartaSansFontFamily
            },
            fontWeight = when (style) {
                KeyStyle.PRIMARY_EQUALS, KeyStyle.OPERATOR -> FontWeight.Bold
                KeyStyle.DIGIT -> FontWeight.SemiBold
                else -> FontWeight.Medium
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GlassPillChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) ElectricBlue.copy(alpha = 0.35f) else Color(0x16FFFFFF),
        label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) CyanGlow.copy(alpha = 0.85f) else GlassBorderSubtle,
        label = "chip_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) CyanGlow else TextSecondary,
        label = "chip_text"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .defaultMinSize(minHeight = 32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
