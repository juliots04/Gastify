package com.example.gastify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.gastify.ui.theme.AmoledBlack
import com.example.gastify.ui.theme.PrimaryGreen

@Composable
fun GastifyHeader(
    scrollFraction: Float = 0f
) {

    val bgAlpha = scrollFraction.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                AmoledBlack.copy(alpha = bgAlpha)
            ),
        contentAlignment = Alignment.CenterStart
    ) {

        Text(
            text = "GASTIFY",
            color = PrimaryGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(
                start = 20.dp
            )
        )
    }
}
