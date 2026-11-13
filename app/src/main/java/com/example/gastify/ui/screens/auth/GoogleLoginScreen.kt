package com.example.gastify.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .safeDrawingPadding()
            .padding(24.dp)
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // IDENTIDAD

            Text(
                text = "GASTIFY",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = PrimaryGreen
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Tus finanzas, bajo control.",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(48.dp)
            )

            // BIENVENIDA

            Text(
                text = "Bienvenido",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Organiza tus gastos, ingresos y negocios en un solo lugar.",
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )

            Spacer(
                modifier = Modifier.height(36.dp)
            )

            // ÚNICO BOTÓN DE AUTENTICACIÓN

            Button(
                onClick = onSignIn,

                enabled = !isLoading,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),

                shape = RoundedCornerShape(16.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color.DarkGray
                ),

                contentPadding = PaddingValues(
                    horizontal = 16.dp
                )
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )

                } else {

                    Image(
                        painter = painterResource(
                            id = R.drawable.ic_google
                        ),

                        contentDescription = null,

                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(14.dp)
                    )

                    Text(
                        text = "Continuar con Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "Inicia sesión o crea tu cuenta automáticamente.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )

            // ERRORES

            if (errorMessage != null) {

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    ),

                    shape = RoundedCornerShape(14.dp),

                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = errorMessage,

                        modifier = Modifier.padding(16.dp),

                        fontSize = 13.sp,
                        lineHeight = 19.sp,

                        textAlign = TextAlign.Center,

                        color = ExpenseRed
                    )
                }
            }
        }

        // PIE DE PANTALLA

        Text(
            text = "GASTIFY • Finanzas personales",

            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),

            fontSize = 11.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
