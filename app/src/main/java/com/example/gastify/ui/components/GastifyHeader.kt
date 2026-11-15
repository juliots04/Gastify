package com.example.gastify.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastify.data.model.Workspace
import com.example.gastify.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastifyHeader(
    scrollFraction: Float = 0f,
    photoUrl: String? = null,
    workspaces: List<Workspace> = emptyList(),
    activeWorkspace: Workspace? = null,
    onSelectWorkspace: (Workspace) -> Unit = {},
    onManageWorkspaces: () -> Unit = {},
    onOpenAccount: () -> Unit = {}
) {
    val avatar by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = photoUrl) {
        value = loadGoogleAvatar(photoUrl)
    }

    var showWorkspaceSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AmoledBlack.copy(alpha = scrollFraction.coerceIn(0f, 1f)))
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "GASTIFY",
            color = PrimaryGreen,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.width(10.dp))

        // Píldora selector global de espacio
        if (activeWorkspace != null) {
            Surface(
                onClick = { showWorkspaceSheet = true },
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (activeWorkspace.isBusiness) Icons.Outlined.Storefront else Icons.Outlined.Person,
                        contentDescription = null,
                        tint = if (activeWorkspace.isBusiness) Color(0xFF64B5F6) else PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = activeWorkspace.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 115.dp)
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Cambiar espacio",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

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

    // Modal para cambio rápido de espacio
    if (showWorkspaceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWorkspaceSheet = false },
            containerColor = SurfaceDark,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cambiar espacio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    TextButton(
                        onClick = {
                            showWorkspaceSheet = false
                            onManageWorkspaces()
                        }
                    ) {
                        Text(
                            text = "Gestionar",
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (workspaces.isEmpty()) {
                    Text(
                        text = "No tienes espacios creados aún.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        workspaces.forEach { workspace ->
                            val isSelected = workspace.id == activeWorkspace?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) PrimaryGreen.copy(alpha = 0.12f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .clickable {
                                        onSelectWorkspace(workspace)
                                        showWorkspaceSheet = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (workspace.isBusiness) Color(0xFF64B5F6).copy(alpha = 0.15f)
                                            else PrimaryGreen.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (workspace.isBusiness) Icons.Outlined.Storefront else Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = if (workspace.isBusiness) Color(0xFF64B5F6) else PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = workspace.name,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(Modifier.width(8.dp))

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (workspace.isBusiness) Color(0xFF64B5F6).copy(alpha = 0.18f) else PrimaryGreen.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = workspace.typeLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (workspace.isBusiness) Color(0xFF90CAF9) else PrimaryGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = if (workspace.isBusiness) "Todos los módulos habilitados" else "Cuentas, categorías, recordatorios y presupuestos",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Seleccionado",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        showWorkspaceSheet = false
                        onManageWorkspaces()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Icon(
                        imageVector = GastifyIcons.settings,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Configurar espacios en Ajustes",
                        color = PrimaryGreen,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
