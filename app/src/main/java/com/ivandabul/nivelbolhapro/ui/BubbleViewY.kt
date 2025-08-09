package com.ivandabul.nivelbolhapro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun BubbleViewY(angleDeg: Float, maxAngle: Float, width: Dp, height: Dp) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val bubbleColor = Color(0xFFC8E6C9)
    Box(
        modifier = Modifier.width(width).height(height).border(1.dp, MaterialTheme.colorScheme.outline).background(trackColor).padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(modifier = Modifier.align(Alignment.Center).fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
        }
        val usableHeight = height - 24.dp
        val progress = (angleDeg / maxAngle).coerceIn(-1f, 1f) * 0.9f
        val offsetY = usableHeight * progress
        Box(modifier = Modifier.size(min(width - 12.dp, 28.dp)).offset(x = 0.dp, y = offsetY).shadow(6.dp, CircleShape).clip(CircleShape).background(bubbleColor))
    }
}