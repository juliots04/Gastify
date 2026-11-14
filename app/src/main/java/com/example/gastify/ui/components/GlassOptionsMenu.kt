package com.example.gastify.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.GlassOptics
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.OpticalSizeValue
import dev.chrisbanes.haze.glass.hazeGlass

/** Context menu whose glass samples the content beneath it. */
@Composable
fun GlassOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    glassState: HazeState,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    val glassStyle = remember {
        GlassStyle {
            shape(shape)
            tint(Color(0xFF172232).copy(alpha = 0.30f))
            optics(GlassOptics.regular.copy(refractionStrength = 0.55f, blurRadius = OpticalSizeValue.Fixed(12.dp)))
            specularIntensity(0.38f)
            ambientResponse(0.16f)
            edgeSoftness(2.dp)
            alpha(0.45f)
        }
    }
    val interiorBlurStyle = remember {
        HazeBlurStyle {
            blurEnabled(true)
            blurRadius(12.dp)
        }
    }
    val scaleX = animateFloatAsState(
        targetValue = if (expanded) 1f else 0.90f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
        label = "glass menu width"
    )
    val scaleY = animateFloatAsState(
        targetValue = if (expanded) 1f else 0.78f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "glass menu height"
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        modifier = Modifier
            .width(216.dp)
            .graphicsLayer {
                this.scaleX = scaleX.value
                this.scaleY = scaleY.value
                transformOrigin = TransformOrigin(0.9f, 0f)
            }
            .clip(shape)
            .hazeBlur(input = HazeInput.Sources(glassState), style = interiorBlurStyle)
            .hazeGlass(input = HazeInput.Sources(glassState), style = glassStyle),
        content = content
    )
}

@Composable
fun GlassOptionsItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val color = if (destructive) Color(0xFFFFA6A6) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f), color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun GlassOptionsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.18f)
    )
}