package com.example.gastify.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastify.R
import com.example.gastify.ui.theme.*

@Composable
fun GoogleLoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(AmoledBlack).safeDrawingPadding()
    ) {
        FinanceBackground(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GASTIFY",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = PrimaryGreen
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tus finanzas, bajo control.",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Organiza tus gastos, ingresos y negocios en un solo lugar.",
                modifier = Modifier.widthIn(max = 300.dp),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onSignIn,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color.DarkGray
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text("Continuar con Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Inicia sesión o crea tu cuenta automáticamente.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
            if (errorMessage != null) {
                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp)
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val ink = PrimaryGreen.copy(alpha = 0.12f)
        val faint = PrimaryGreen.copy(alpha = 0.06f)
        val stroke = 1.5.dp.toPx()
        val unit = minOf(size.width, size.height)
        val chart = Offset(size.width * .15f, size.height * .15f)
        val chartSize = unit * .18f
        drawRoundRect(
            color = ink,
            topLeft = chart,
            size = Size(chartSize, chartSize * .72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
            style = Stroke(stroke)
        )
        val points = listOf(
            Offset(chart.x + chartSize * .15f, chart.y + chartSize * .53f),
            Offset(chart.x + chartSize * .38f, chart.y + chartSize * .36f),
            Offset(chart.x + chartSize * .57f, chart.y + chartSize * .43f),
            Offset(chart.x + chartSize * .83f, chart.y + chartSize * .19f)
        )
        points.zipWithNext { start, end ->
            drawLine(ink, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
        }
        val coin = Offset(size.width * .81f, size.height * .21f)
        drawCircle(faint, radius = unit * .075f, center = coin)
        drawCircle(ink, radius = unit * .075f, center = coin, style = Stroke(stroke))
        drawLine(ink, Offset(coin.x, coin.y - unit * .035f), Offset(coin.x, coin.y + unit * .035f), stroke, cap = StrokeCap.Round)
        drawCircle(ink, radius = unit * .025f, center = Offset(size.width * .17f, size.height * .78f), style = Stroke(stroke))
        drawCircle(faint, radius = unit * .12f, center = Offset(size.width * .83f, size.height * .82f), style = Stroke(stroke))
        val baseX = size.width * .75f
        val baseY = size.height * .85f
        listOf(.05f, .09f, .14f).forEachIndexed { index, height ->
            drawLine(
                ink,
                Offset(baseX + index * unit * .045f, baseY),
                Offset(baseX + index * unit * .045f, baseY - unit * height),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCircle(ink, radius = 2.dp.toPx(), center = Offset(size.width * .32f, size.height * .88f))
        drawCircle(ink, radius = 2.dp.toPx(), center = Offset(size.width * .68f, size.height * .11f))
    }
}