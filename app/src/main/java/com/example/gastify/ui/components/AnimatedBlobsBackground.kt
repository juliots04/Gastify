package com.example.gastify.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

import androidx.compose.foundation.Canvas

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import com.example.gastify.ui.theme.PrimaryGreen
import com.example.gastify.ui.theme.SecondaryPurple

import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedBlobsBackground(
    modifier: Modifier = Modifier
) {

    val transition = rememberInfiniteTransition(
        label = "blobs"
    )

    // ======================================
    // ÁNGULOS DE ÓRBITA
    // ======================================

    val a1 = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "a1"
    )

    val a2 = transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "a2"
    )

    val a3 = transition.animateFloat(
        initialValue = 90f,
        targetValue = 450f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "a3"
    )

    val a4 = transition.animateFloat(
        initialValue = 200f,
        targetValue = 560f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "a4"
    )

    val a5 = transition.animateFloat(
        initialValue = 300f,
        targetValue = 660f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "a5"
    )

    val pulse = transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pulse2 = transition.animateFloat(
        initialValue = 1.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    // ======================================
    // COLORES
    // ======================================

    val blobGreen = PrimaryGreen.copy(alpha = 0.10f)
    val blobPurple = SecondaryPurple.copy(alpha = 0.09f)
    val blobTeal = Color(0xFF06B6D4).copy(alpha = 0.08f)
    val blobPink = Color(0xFFEC4899).copy(alpha = 0.07f)
    val blobAmber = Color(0xFFF59E0B).copy(alpha = 0.06f)

    // ======================================
    // CANVAS
    // ======================================

    Canvas(modifier = modifier) {

        val w = size.width
        val h = size.height

        // ---- Mancha 1: verde (superior izquierda) ----
        val r1 = Math.toRadians(a1.value.toDouble())
        val cx1 = w * 0.2f + cos(r1).toFloat() * w * 0.1f
        val cy1 = h * 0.12f + sin(r1).toFloat() * h * 0.06f
        val radius1 = w * 0.35f * pulse.value

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blobGreen, Color.Transparent),
                center = Offset(cx1, cy1),
                radius = radius1
            ),
            radius = radius1,
            center = Offset(cx1, cy1)
        )

        // ---- Mancha 2: púrpura (derecha) ----
        val r2 = Math.toRadians(a2.value.toDouble())
        val cx2 = w * 0.8f + cos(r2).toFloat() * w * 0.08f
        val cy2 = h * 0.18f + sin(r2).toFloat() * h * 0.05f
        val radius2 = w * 0.3f * pulse2.value

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blobPurple, Color.Transparent),
                center = Offset(cx2, cy2),
                radius = radius2
            ),
            radius = radius2,
            center = Offset(cx2, cy2)
        )

        // ---- Mancha 3: teal (centro) ----
        val r3 = Math.toRadians(a3.value.toDouble())
        val cx3 = w * 0.5f + cos(r3).toFloat() * w * 0.12f
        val cy3 = h * 0.3f + sin(r3).toFloat() * h * 0.04f
        val radius3 = w * 0.28f * pulse.value

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blobTeal, Color.Transparent),
                center = Offset(cx3, cy3),
                radius = radius3
            ),
            radius = radius3,
            center = Offset(cx3, cy3)
        )

        // ---- Mancha 4: rosa (inferior izquierda) ----
        val r4 = Math.toRadians(a4.value.toDouble())
        val cx4 = w * 0.3f + cos(r4).toFloat() * w * 0.1f
        val cy4 = h * 0.45f + sin(r4).toFloat() * h * 0.05f
        val radius4 = w * 0.25f * pulse2.value

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blobPink, Color.Transparent),
                center = Offset(cx4, cy4),
                radius = radius4
            ),
            radius = radius4,
            center = Offset(cx4, cy4)
        )

        // ---- Mancha 5: ámbar (inferior derecha) ----
        val r5 = Math.toRadians(a5.value.toDouble())
        val cx5 = w * 0.75f + cos(r5).toFloat() * w * 0.09f
        val cy5 = h * 0.55f + sin(r5).toFloat() * h * 0.04f
        val radius5 = w * 0.22f * pulse.value

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blobAmber, Color.Transparent),
                center = Offset(cx5, cy5),
                radius = radius5
            ),
            radius = radius5,
            center = Offset(cx5, cy5)
        )
    }
}

