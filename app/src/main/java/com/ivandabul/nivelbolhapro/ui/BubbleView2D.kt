package com.ivandabul.nivelbolhapro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun BubbleView2D(angleXDeg: Float, angleYDeg: Float, maxAngle: Float, size: Dp) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val bubbleColor = Color(0xFFFFF59D)

    val radius = size / 2 - 12.dp
    val progressX = (angleXDeg / maxAngle).coerceIn(-1f, 1f) * 0.9f
    val progressY = (angleYDeg / maxAngle).coerceIn(-1f, 1f) * 0.9f

    Box(modifier = Modifier.size(size).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).background(trackColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 12.dp)) {
            val r = size.toPx() / 2 - 12.dp.toPx()
            drawCircle(color = MaterialTheme.colorScheme.outline, radius = r, style = Stroke(width = 2f))
            drawCircle(color = MaterialTheme.colorScheme.outline, radius = r * 0.66f, style = Stroke(width = 1.5f))
            drawCircle(color = MaterialTheme.colorScheme.outline, radius = r * 0.33f, style = Stroke(width = 1.5f))
            drawLine(MaterialTheme.colorScheme.outline, start = androidx.compose.ui.geometry.Offset(-r, 0f), end = androidx.compose.ui.geometry.Offset(r, 0f), strokeWidth = 1f)
            drawLine(MaterialTheme.colorScheme.outline, start = androidx.compose.ui.geometry.Offset(0f, -r), end = androidx.compose.ui.geometry.Offset(0f, r), strokeWidth = 1f)
        }
        val offX = radius * progressX
        val offY = radius * progressY
        Box(modifier = Modifier.size(min(36.dp, radius)).shadow(6.dp, CircleShape).clip(CircleShape).background(bubbleColor).align(Alignment.Center).offset(x = offX, y = offY))
    }
}