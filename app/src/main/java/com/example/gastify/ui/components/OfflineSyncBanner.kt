package com.example.gastify.ui.components

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.gastify.data.repository.FirestoreConnectionStatus
import com.example.gastify.data.repository.FirestoreSyncState

import com.example.gastify.ui.theme.*

import kotlinx.coroutines.delay

// ==========================================
// COLORES
// ==========================================

private val OfflineWarningColor =
    Color(0xFFFFBF69)

// ==========================================
// AVISO DE SINCRONIZACIÓN
// ==========================================

@Composable
fun OfflineSyncBanner(
    state: FirestoreSyncState
) {

    // ======================================
    // DETERMINAR SI HAY UN AVISO
    // ======================================

    val hasWarning =

        state.connection !=
            FirestoreConnectionStatus.SERVER

            ||

        state.isVerifyingWrites

            ||

        state.errorMessage != null

    var isVisible by remember {

        mutableStateOf(false)
    }

    // ======================================
    // EVITAR PARPADEOS
    // ======================================

    LaunchedEffect(hasWarning) {

        if (!hasWarning) {

            isVisible = false

        } else {

            // Las consultas normales de Firestore
            // suelen pasar rápidamente por caché.
            // Evitamos mostrar el aviso durante
            // esas transiciones breves.

            delay(700)

            isVisible = true
        }
    }

    if (!isVisible || !hasWarning) {
        return
    }

    // ======================================
    // PREPARAR MENSAJES
    // ======================================

    val title: String

    val description: String

    val indicatorColor: Color

    val showProgress: Boolean

    when {

        // ==================================
        // ERROR
        // ==================================

        state.connection ==
            FirestoreConnectionStatus.ERROR -> {

            title = "Problema de sincronización"

            description =
                state.errorMessage
                    ?: "No se pudo consultar Firebase."

            indicatorColor = ExpenseRed

            showProgress = false
        }

        // ==================================
        // DATOS DE CACHÉ
        // ==================================

        state.connection ==
            FirestoreConnectionStatus.CACHE -> {

            title = "Consultando datos guardados"

            description = if (
                state.hasPendingWorkspaceWrites
            ) {

                "Hay cambios pendientes en la consulta de espacios. " +
                    "Firebase intentará confirmarlos."

            } else {

                "La consulta de espacios todavía no recibe " +
                    "confirmación del servidor."
            }

            indicatorColor = OfflineWarningColor

            showProgress = false
        }

        // ==================================
        // ERROR DE VERIFICACIÓN
        // ==================================

        state.errorMessage != null -> {

            title = "Sincronización pendiente"

            description =
                state.errorMessage

            indicatorColor = OfflineWarningColor

            showProgress = false
        }

        // ==================================
        // VERIFICANDO ESCRITURAS
        // ==================================

        state.isVerifyingWrites -> {

            title = "Verificando sincronización"

            description =
                "Comprobando las escrituras pendientes."

            indicatorColor = PrimaryGreen

            showProgress = true
        }

        // ==================================
        // COMPROBANDO CONEXIÓN
        // ==================================

        else -> {

            title = "Comprobando Firebase"

            description =
                "Verificando la disponibilidad del servidor."

            indicatorColor = PrimaryGreen

            showProgress = true
        }
    }

    // ======================================
    // INTERFAZ
    // ======================================

    Surface(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),

        shape = RoundedCornerShape(16.dp),

        color = SurfaceDark,

        tonalElevation = 0.dp

    ) {

        Row(

            modifier = Modifier.padding(14.dp),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            // ==================================
            // INDICADOR
            // ==================================

            if (showProgress) {

                CircularProgressIndicator(

                    modifier = Modifier.size(19.dp),

                    color = indicatorColor,

                    strokeWidth = 2.dp
                )

            } else {

                Surface(

                    modifier = Modifier.size(12.dp),

                    shape = CircleShape,

                    color = indicatorColor

                ) {}
            }

            Spacer(
                Modifier.width(12.dp)
            )

            // ==================================
            // CONTENIDO
            // ==================================

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(

                    text = title,

                    color = indicatorColor,

                    fontSize = 13.sp,

                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(

                    text = description,

                    color = TextSecondary,

                    fontSize = 12.sp,

                    lineHeight = 18.sp
                )

                // ==================================
                // ADVERTENCIA FINANCIERA
                // ==================================

                if (
                    state.connection ==
                    FirestoreConnectionStatus.CACHE
                ) {

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(

                        text =
                            "Las compras, ventas y transacciones financieras requieren internet.",

                        color = OfflineWarningColor,

                        fontSize = 11.sp,

                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
