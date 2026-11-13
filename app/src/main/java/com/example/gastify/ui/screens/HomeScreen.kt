package com.example.gastify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.gastify.ui.theme.*

@Composable
fun GastifyHomeScreen(
    modifier: Modifier = Modifier,
    workspaces: List<String>
) {

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "GASTIFY",
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryGreen
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Tus finanzas, bajo control.",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // BALANCE PRINCIPAL

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            colors = CardDefaults.cardColors(
                containerColor = SurfaceDark
            )
        ) {

            Column(
                modifier = Modifier.padding(24.dp)
            ) {

                Text(
                    text = "Balance disponible",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "S/ 0.00",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "Resumen general de tus finanzas",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // INGRESOS Y GASTOS

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FinanceSummaryCard(
                title = "Ingresos",
                amount = "S/ 0.00",
                accentColor = IncomeGreen,
                modifier = Modifier.weight(1f)
            )

            FinanceSummaryCard(
                title = "Gastos",
                amount = "S/ 0.00",
                accentColor = ExpenseRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(
            modifier = Modifier.height(34.dp)
        )

        // ESPACIOS

        Text(
            text = "Mis espacios",
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "${workspaces.size} espacios registrados",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (workspaces.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(20.dp),

                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark
                )
            ) {

                Column(
                    modifier = Modifier.padding(22.dp)
                ) {

                    Text(
                        text = "Sin espacios registrados",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Crea un espacio para organizar tus gastos personales, pasajes o negocios.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = TextSecondary
                    )
                }
            }

        } else {

            workspaces.forEach { workspace ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),

                    shape = RoundedCornerShape(18.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),

                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = workspace,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Activo",
                            color = PrimaryGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        // MOVIMIENTOS

        Text(
            text = "Movimientos recientes",
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(
                containerColor = SurfaceDark
            )
        ) {

            Text(
                text = "Aún no hay movimientos registrados.",
                modifier = Modifier.padding(22.dp),
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
private fun FinanceSummaryCard(
    title: String,
    amount: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = title,
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = amount,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
