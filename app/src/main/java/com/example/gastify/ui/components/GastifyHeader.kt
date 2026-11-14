package com.example.gastify.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastify.ui.theme.AmoledBlack
import com.example.gastify.ui.theme.PrimaryGreen

@Composable
fun GastifyHeader(scrollFraction: Float = 0f, photoUrl: String? = null, onOpenAccount: () -> Unit = {}) {
    val avatar by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = photoUrl) {
        value = loadGoogleAvatar(photoUrl)
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).background(AmoledBlack.copy(alpha = scrollFraction.coerceIn(0f, 1f))).padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("GASTIFY", modifier = Modifier.weight(1f), color = PrimaryGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        IconButton(onClick = onOpenAccount, modifier = Modifier.size(44.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                val bitmap = avatar
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), contentDescription = "Abrir cuenta", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Outlined.Person, contentDescription = "Abrir cuenta", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}