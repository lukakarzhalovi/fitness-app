package com.example.final_exam_project.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun GoalRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    // animateFloatAsState watches 'progress' and smoothly interpolates to its new
    // value whenever it changes (e.g. after a workout is logged). Without this the
    // arc would jump instantly instead of sweeping around the ring.
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "goalRingProgress"
    )

    // Read colors outside the Canvas lambda. Canvas is a DrawScope, not a
    // Composable scope, so MaterialTheme cannot be accessed inside it.
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 24.dp.toPx()

            // Shrink the arc bounds inward by half the stroke width on each side
            // so the thick stroke is fully visible and not clipped at the edges.
            val inset = strokeWidth / 2f
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

            // 1. Grey background track — a full 360° arc so the ring shape is
            //    always visible even when progress is 0.
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Colored progress arc.
            //    startAngle = 270° because Android's coordinate system places 0°
            //    at the 3-o'clock position; 270° is the 12-o'clock (top) position
            //    where a progress ring naturally begins.
            //    sweepAngle = fraction of 360° already completed.
            if (animatedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = 270f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Percentage label centred inside the ring. Uses the non-animated 'progress'
        // so the number updates once the data arrives, not mid-animation.
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
