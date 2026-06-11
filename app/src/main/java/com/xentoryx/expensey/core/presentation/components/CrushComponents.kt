package com.xentoryx.expensey.core.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xentoryx.expensey.app.ui.theme.*

@Composable
fun CrushOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isLabelUp = isFocused || value.isNotEmpty()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) CrushLavender else CrushBorder,
        animationSpec = tween(200),
        label = "border"
    )
    val borderWidth by animateFloatAsState(
        targetValue = if (isFocused) 1.8f else 1.2f,
        animationSpec = tween(200),
        label = "bw"
    )
    val labelTop by animateDpAsState(
        targetValue = if (isLabelUp) 8.dp else 19.dp,
        animationSpec = tween(170),
        label = "lt"
    )
    val labelSize by animateFloatAsState(
        targetValue = if (isLabelUp) 10.5f else 14f,
        animationSpec = tween(170),
        label = "ls"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isFocused && isLabelUp) CrushLavender else CrushTextSecondary,
        animationSpec = tween(170),
        label = "lc"
    )

    val shape = RoundedCornerShape(14.dp)
    val leadingPad = if (leadingIcon != null) 42.dp else 14.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = TextStyle(
            color = CrushTextPrimary,
            fontSize = 15.sp,
            lineHeight = 15.sp
        ),
        cursorBrush = SolidColor(CrushLavender),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(CrushInputBg, shape)
            .border(borderWidth.dp, borderColor, shape),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize()) {

                // Leading icon
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 13.dp),
                        contentAlignment = Alignment.Center
                    ) { leadingIcon() }
                }

                // Floating label
                Text(
                    text = label,
                    color = labelColor,
                    fontSize = labelSize.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = labelTop, start = leadingPad)
                )

                // Input text
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = leadingPad,
                            end = if (trailingIcon != null) 40.dp else 14.dp,
                            bottom = 7.dp
                        )
                ) { innerTextField() }

                // Trailing icon
                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        contentAlignment = Alignment.Center
                    ) { trailingIcon() }
                }
            }
        }
    )
}

@Composable
fun CrushActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (enabled) 1.0f else 0.97f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ButtonScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = CrushLavender,
            disabledContainerColor = CrushBorder
        ),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = CrushBg,
                    strokeWidth = 3.dp
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = CrushBg
                    )
                )
            }
        }
    }
}

@Composable
fun CrushCanvasDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeColor = CrushTextSecondary.copy(alpha = 0.12f)
        val strokeWidth = 1.5.dp.toPx()

        // 1. Dotted trails
        val trailPath = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.28f)
            cubicTo(
                size.width * 0.25f, size.height * 0.18f,
                size.width * 0.35f, size.height * 0.45f,
                size.width * 0.18f, size.height * 0.65f
            )
            cubicTo(
                size.width * 0.05f, size.height * 0.78f,
                size.width * 0.45f, size.height * 0.88f,
                size.width * 0.85f, size.height * 0.72f
            )
        }
        drawPath(
            path = trailPath,
            color = CrushTextSecondary.copy(alpha = 0.08f),
            style = Stroke(
                width = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )

        // 2. Sun
        val sunCenter = Offset(size.width * 0.86f, size.height * 0.14f)
        val sunRadius = 24.dp.toPx()
        drawCircle(
            color = CrushYellow.copy(alpha = 0.12f),
            center = sunCenter,
            radius = sunRadius
        )
        drawCircle(
            color = strokeColor,
            center = sunCenter,
            radius = sunRadius,
            style = Stroke(width = strokeWidth)
        )
        for (i in 0 until 8) {
            val angle = i * (360f / 8) * (Math.PI / 180f)
            val start = Offset(
                x = (sunCenter.x + (sunRadius + 5.dp.toPx()) * Math.cos(angle)).toFloat(),
                y = (sunCenter.y + (sunRadius + 5.dp.toPx()) * Math.sin(angle)).toFloat()
            )
            val end = Offset(
                x = (sunCenter.x + (sunRadius + 13.dp.toPx()) * Math.cos(angle)).toFloat(),
                y = (sunCenter.y + (sunRadius + 13.dp.toPx()) * Math.sin(angle)).toFloat()
            )
            drawLine(
                color = strokeColor,
                start = start,
                end = end,
                strokeWidth = strokeWidth
            )
        }

        // 3. Cloud 1
        val cloudPath = Path().apply {
            moveTo(35.dp.toPx(), 110.dp.toPx())
            quadraticTo(45.dp.toPx(), 95.dp.toPx(), 60.dp.toPx(), 100.dp.toPx())
            quadraticTo(75.dp.toPx(), 85.dp.toPx(), 90.dp.toPx(), 100.dp.toPx())
            quadraticTo(105.dp.toPx(), 100.dp.toPx(), 110.dp.toPx(), 115.dp.toPx())
            quadraticTo(100.dp.toPx(), 130.dp.toPx(), 75.dp.toPx(), 125.dp.toPx())
            quadraticTo(50.dp.toPx(), 128.dp.toPx(), 35.dp.toPx(), 110.dp.toPx())
        }
        drawPath(path = cloudPath, color = CrushPink.copy(alpha = 0.05f))
        drawPath(path = cloudPath, color = strokeColor, style = Stroke(width = strokeWidth))

        // 4. Cloud 2
        val cloud2Path = Path().apply {
            val xOff = size.width - 130.dp.toPx()
            val yOff = size.height - 200.dp.toPx()
            moveTo(xOff + 10.dp.toPx(), yOff + 20.dp.toPx())
            quadraticTo(xOff + 20.dp.toPx(), yOff + 5.dp.toPx(), xOff + 35.dp.toPx(), yOff + 10.dp.toPx())
            quadraticTo(xOff + 50.dp.toPx(), yOff - 5.dp.toPx(), xOff + 65.dp.toPx(), yOff + 10.dp.toPx())
            quadraticTo(xOff + 80.dp.toPx(), yOff + 10.dp.toPx(), xOff + 85.dp.toPx(), yOff + 25.dp.toPx())
            quadraticTo(xOff + 75.dp.toPx(), yOff + 40.dp.toPx(), xOff + 50.dp.toPx(), yOff + 35.dp.toPx())
            quadraticTo(xOff + 25.dp.toPx(), yOff + 38.dp.toPx(), xOff + 10.dp.toPx(), yOff + 20.dp.toPx())
        }
        drawPath(path = cloud2Path, color = CrushPink.copy(alpha = 0.04f))
        drawPath(path = cloud2Path, color = strokeColor, style = Stroke(width = strokeWidth))

        // 5. Paper plane
        val planePath = Path().apply {
            moveTo(30.dp.toPx(), size.height * 0.38f)
            lineTo(55.dp.toPx(), size.height * 0.38f - 12.dp.toPx())
            lineTo(44.dp.toPx(), size.height * 0.38f + 10.dp.toPx())
            close()
            moveTo(44.dp.toPx(), size.height * 0.38f + 10.dp.toPx())
            lineTo(48.dp.toPx(), size.height * 0.38f + 4.dp.toPx())
        }
        drawPath(path = planePath, color = strokeColor, style = Stroke(width = strokeWidth))

        // 6. Stars
        fun drawStar(offset: Offset) {
            val starPath = Path().apply {
                moveTo(offset.x, offset.y - 7.dp.toPx())
                quadraticTo(offset.x, offset.y, offset.x + 7.dp.toPx(), offset.y)
                quadraticTo(offset.x, offset.y, offset.x, offset.y + 7.dp.toPx())
                quadraticTo(offset.x, offset.y, offset.x - 7.dp.toPx(), offset.y)
                quadraticTo(offset.x, offset.y, offset.x, offset.y - 7.dp.toPx())
            }
            drawPath(path = starPath, color = CrushYellow.copy(alpha = 0.15f))
            drawPath(
                path = starPath,
                color = CrushYellow.copy(alpha = 0.4f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        drawStar(Offset(size.width * 0.14f, size.height * 0.62f))
        drawStar(Offset(size.width * 0.88f, size.height * 0.42f))
        drawStar(Offset(size.width * 0.35f, size.height * 0.12f))
        drawStar(Offset(size.width * 0.72f, size.height * 0.82f))
    }
}
