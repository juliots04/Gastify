package com.example.gastify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastify.ui.theme.BorderDark
import com.example.gastify.ui.theme.PrimaryGreen
import com.example.gastify.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

@Composable
fun BalanceSummaryCard(
    title: String,
    amount: String,
    backgroundState: HazeState,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val blurStyle = remember {
        HazeBlurStyle {
            blurRadius(24.dp)
            colorEffects(listOf(HazeColorEffect.tint(Color(0xFF0C1119).copy(alpha = 0.78f))))
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .hazeBlur(input = HazeInput.Sources(backgroundState), style = blurStyle)
            .background(Brush.linearGradient(listOf(PrimaryGreen.copy(alpha = 0.06f), Color.Transparent, Color.White.copy(alpha = 0.025f))))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(PrimaryGreen.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(GastifyIcons.accounts, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, fontSize = 14.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            Text(amount, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = BorderDark)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}