package com.example.gastify.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Verified

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.ui.theme.*
import com.example.gastify.viewmodel.ProfileViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

// ==========================================
// DESCARGAR FOTO DE GOOGLE
// ==========================================

private suspend fun loadGoogleAvatar(
    photoUrl: String?
): Bitmap? = withContext(Dispatchers.IO) {

    if (photoUrl.isNullOrBlank()) {
        return@withContext null
    }

    var connection: HttpURLConnection? = null

    try {

        val url = URL(photoUrl)

        // Solo permitir conexiones seguras.

        if (
            url.protocol.lowercase() != "https"
        ) {
            return@withContext null
        }

        connection = url.openConnection()
            as? HttpURLConnection
            ?: return@withContext null

        connection.connectTimeout = 7000
        connection.readTimeout = 7000

        connection.instanceFollowRedirects = true

        // Limitar la descarga a 2 MB.

        val output = ByteArrayOutputStream()

        connection.inputStream.use { input ->

            val buffer = ByteArray(8192)

            var total = 0

            while (true) {

                val count = input.read(buffer)

                if (count < 0) {
                    break
                }

                if (count == 0) {
                    continue
                }

                total += count

                if (total > 2_000_000) {
                    return@withContext null
                }

                output.write(
                    buffer,
                    0,
                    count
                )
            }
        }

        val bytes = output.toByteArray()

        // Consultar dimensiones.

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            bounds
        )

        if (
            bounds.outWidth <= 0 ||
            bounds.outHeight <= 0
        ) {
            return@withContext null
        }

        // Reducir imágenes grandes.

        var sampleSize = 1

        while (
            maxOf(
                bounds.outWidth / sampleSize,
                bounds.outHeight / sampleSize
            ) > 512
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {

            inSampleSize = sampleSize

            inPreferredConfig =
                Bitmap.Config.ARGB_8888
        }

        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            options
        )

    } catch (_: Exception) {

        null

    } finally {

        connection?.disconnect()
    }
}

// ==========================================
// PERFIL DE USUARIO
// ==========================================

@Composable
fun ProfileSettingsSection(
    userId: String
) {

    val profileVm: ProfileViewModel = viewModel(

        key = "profile_$userId",

        factory = ProfileViewModel.factory(
            userId
        )
    )

    val state by profileVm
        .uiState
        .collectAsState()

    // ======================================
    // ACTUALIZAR PERFIL AL ENTRAR
    // ======================================

    LaunchedEffect(profileVm) {

        profileVm.refreshProfile()
    }

    // ======================================
    // CARGAR FOTO AUTOMÁTICAMENTE
    // ======================================

    val googleAvatar by produceState<Bitmap?>(

        initialValue = null,

        key1 = state.photoUrl
    ) {

        value = loadGoogleAvatar(
            state.photoUrl
        )
    }

    // ======================================
    // INICIAL DEL NOMBRE
    // ======================================

    val initial = state.displayName
        .trim()
        .take(1)
        .uppercase(Locale.ROOT)
        .ifBlank {
            "U"
        }

    // ======================================
    // INTERFAZ
    // ======================================

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.AccountCircle,

                contentDescription = null,

                tint = PrimaryGreen,

                modifier = Modifier.size(27.dp)
            )

            Spacer(
                Modifier.width(10.dp)
            )

            Text(
                text = "Mi perfil",

                fontSize = 20.sp,

                fontWeight = FontWeight.Bold,

                color = TextPrimary
            )
        }

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            text = "Información de tu cuenta de Google.",

            color = TextSecondary,

            fontSize = 13.sp
        )

        Spacer(
            Modifier.height(20.dp)
        )

        // ======================================
        // TARJETA PRINCIPAL
        // ======================================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(22.dp),

            colors = CardDefaults.cardColors(
                containerColor = SurfaceDark
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                // ==============================
                // FOTO DE GOOGLE
                // ==============================

                if (googleAvatar != null) {

                    Image(
                        bitmap =
                            googleAvatar!!.asImageBitmap(),

                        contentDescription =
                            "Foto de perfil de Google",

                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape),

                        contentScale =
                            ContentScale.Crop
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(CardDark),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = initial,

                            color = PrimaryGreen,

                            fontSize = 44.sp,

                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    Modifier.height(20.dp)
                )

                // ==============================
                // NOMBRE COMPLETO DE GOOGLE
                // ==============================

                Text(
                    text = state.displayName,

                    fontSize = 22.sp,

                    fontWeight = FontWeight.Bold,

                    color = TextPrimary,

                    textAlign = TextAlign.Center
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                // ==============================
                // CORREO
                // ==============================

                Text(
                    text = state.email,

                    fontSize = 13.sp,

                    color = TextSecondary,

                    textAlign = TextAlign.Center
                )

                Spacer(
                    Modifier.height(20.dp)
                )

                // ==============================
                // ESTADO DE GOOGLE
                // ==============================

                Surface(
                    color = CardDark,

                    shape = RoundedCornerShape(14.dp)
                ) {

                    Row(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Verified,

                            contentDescription = null,

                            tint = PrimaryGreen,

                            modifier = Modifier.size(19.dp)
                        )

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        Text(
                            text = if (
                                state.isGoogleAccount
                            ) {

                                "Cuenta vinculada a Google"

                            } else {

                                "Cuenta de Firebase"
                            },

                            color = PrimaryGreen,

                            fontSize = 12.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }

                // ==============================
                // ACTUALIZANDO
                // ==============================

                if (state.isLoading) {

                    Spacer(
                        Modifier.height(18.dp)
                    )

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),

                        color = PrimaryGreen,

                        strokeWidth = 2.dp
                    )
                }

                // ==============================
                // ERROR
                // ==============================

                if (state.errorMessage != null) {

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Text(
                        text = state.errorMessage.orEmpty(),

                        color = ExpenseRed,

                        fontSize = 12.sp,

                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(
            Modifier.height(20.dp)
        )

        // ======================================
        // INFORMACIÓN PERSONAL
        // ======================================

        Text(
            text = "Información personal",

            fontSize = 18.sp,

            fontWeight = FontWeight.SemiBold,

            color = TextPrimary
        )

        Spacer(
            Modifier.height(14.dp)
        )

        // ======================================
        // NOMBRE
        // ======================================

        ProfileInfoCard(
            title = "Nombre completo",

            value = state.displayName,

            icon = Icons.Outlined.Person
        )

        Spacer(
            Modifier.height(12.dp)
        )

        // ======================================
        // CORREO
        // ======================================

        ProfileInfoCard(
            title = "Correo electrónico",

            value = state.email,

            icon = Icons.Outlined.Email
        )

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            text = if (state.isGoogleAccount) {

                "El nombre y la fotografía se obtienen del perfil de Google vinculado a tu cuenta. No necesitas guardarlos manualmente."

            } else {

                "Esta cuenta no tiene un perfil de Google vinculado. Se muestran los datos disponibles en Firebase."
            },

            fontSize = 12.sp,

            lineHeight = 19.sp,

            color = TextSecondary
        )
    }
}

// ==========================================
// TARJETA DE INFORMACIÓN
// ==========================================

@Composable
private fun ProfileInfoCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Row(
            modifier = Modifier.padding(18.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,

                contentDescription = null,

                tint = PrimaryGreen,

                modifier = Modifier.size(23.dp)
            )

            Spacer(
                Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,

                    color = TextSecondary,

                    fontSize = 12.sp
                )

                Spacer(
                    Modifier.height(7.dp)
                )

                Text(
                    text = value.ifBlank {
                        "No disponible"
                    },

                    color = TextPrimary,

                    fontSize = 15.sp,

                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
