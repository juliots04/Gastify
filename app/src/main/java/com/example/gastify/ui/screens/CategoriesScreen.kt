package com.example.gastify.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.model.Category
import com.example.gastify.data.model.CategoryType
import com.example.gastify.data.model.Workspace
import com.example.gastify.ui.components.GlassOptionsMenu
import com.example.gastify.ui.components.GlassOptionsItem
import com.example.gastify.ui.components.GlassOptionsDivider
import com.example.gastify.ui.components.rememberSavedViewPreference
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.components.categoryIconOptions
import com.example.gastify.ui.theme.*
import com.example.gastify.viewmodel.CategoryViewModel

import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val categoryColors = listOf(
    "#00E0A4",
    "#A78BFA",
    "#60A5FA",
    "#F59E0B",
    "#FF5C75",
    "#FFFFFF"
)

private fun categoryTint(value: String): Color {

    return try {
        Color(android.graphics.Color.parseColor(value))
    } catch (_: Exception) {
        PrimaryGreen
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit
) {

    val vm: CategoryViewModel = viewModel(
        key = "categories_${userId}_${workspace.id}",
        factory = CategoryViewModel.factory(
            userId,
            workspace.id
        )
    )

    val state by vm.uiState.collectAsState()

    DisposableEffect(vm) {

        vm.startListening()

        onDispose {
            vm.stopListening()
        }
    }

    var showEditor by rememberSaveable {
        mutableStateOf(false)
    }

    var editingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var categoryName by rememberSaveable {
        mutableStateOf("")
    }

    var categoryType by rememberSaveable {
        mutableStateOf(CategoryType.EXPENSE.name)
    }

    var categoryIcon by rememberSaveable {
        mutableStateOf("bus")
    }

    var categoryColor by rememberSaveable {
        mutableStateOf("#00E0A4")
    }

    var toDeleteId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var filter by rememberSaveable {
        mutableStateOf("ALL")
    }

    val snackbar = remember {
        SnackbarHostState()
    }

    LaunchedEffect(state.errorMessage) {

        state.errorMessage?.let {

            snackbar.showSnackbar(it)

            vm.clearError()
        }
    }

    fun openEditor(category: Category?) {

        editingId = category?.id
        categoryName = category?.name ?: ""

        categoryType = category?.type
            ?: CategoryType.EXPENSE.name

        categoryIcon = category?.icon ?: "bus"

        categoryColor = category?.color ?: "#00E0A4"

        showEditor = true
    }

    val visible = remember(state.categories, filter) { state.categories.filter { filter == "ALL" || it.type == filter } }

    val (gridView, setGridView) = rememberSavedViewPreference(userId, "categories_grid", false)
    val glassState = rememberHazeState()
    val listState = rememberLazyGridState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeDistance = with(LocalDensity.current) { 100.dp.toPx() }
    val headerAlpha = if (listState.firstVisibleItemIndex > 0) 1f else
        (listState.firstVisibleItemScrollOffset / fadeDistance).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {

        Box(Modifier.fillMaxSize().hazeSource(glassState).background(AmoledBlack)) {
        AccountsLightsBackground(Modifier.fillMaxSize())

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (gridView) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = statusBarHeight + 76.dp,
                start = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item(span = { GridItemSpan(maxLineSpan) }) {

                Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mis categorías", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { setGridView(!gridView) }) {
                        Icon(
                            imageVector = if (gridView) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                            contentDescription = if (gridView) "Ver en filas" else "Ver en cuadrícula",
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                BoxWithConstraints(
                    modifier = Modifier.widthIn(max = 270.dp).fillMaxWidth().height(40.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f))
                ) {
                    val options = listOf("ALL" to "Todas", "EXPENSE" to "Gastos", "INCOME" to "Ingresos")
                    val segmentWidth = maxWidth / options.size
                    val indicatorOffset by animateDpAsState(
                        targetValue = segmentWidth * options.indexOfFirst { it.first == filter }.coerceAtLeast(0),
                        animationSpec = tween(durationMillis = 280), label = "category indicator"
                    )
                    Box(
                        Modifier.offset(x = indicatorOffset).width(segmentWidth).fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.18f))
                    )
                    Row(Modifier.fillMaxSize()) {
                        options.forEach { (value, label) ->
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { filter = value },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (filter == value) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = if (filter == value) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
                }
            }

            if (state.isLoading) {

                item(span = { GridItemSpan(maxLineSpan) }) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = PrimaryGreen
                        )
                    }
                }

            } else if (visible.isEmpty()) {

                item(span = { GridItemSpan(maxLineSpan) }) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {

                            Text(
                                text = "Sin categorías",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Crea categorías para organizar tus ingresos y gastos.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

            } else {

                gridItems(items = visible, key = { it.id }) { category ->
                    val tint = categoryTint(category.color)
                    var menuExpanded by remember(category.id) { mutableStateOf(false) }
                    Column(
                        modifier = Modifier.animateItem().animateContentSize().fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.05f))
                            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(end = 4.dp, bottom = 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.28f), tint.copy(alpha = 0.08f))))
                                        .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(GastifyIcons.category(category.icon), contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                                }
                                Box(
                                    modifier = Modifier.align(Alignment.BottomEnd).size(20.dp)
                                        .clip(CircleShape).background(Color(0xFF171A20))
                                        .border(1.dp, tint.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (category.type == "INCOME") "+" else "−", color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp)
                                }
                            }
                            if (!gridView) {
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(category.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(5.dp))
                                    Text(if (category.type == "INCOME") "Ingreso · Suma al saldo" else "Gasto · Descuenta del saldo", color = TextSecondary, fontSize = 12.sp)
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            Box {
                                IconButton(onClick = { menuExpanded = true }, enabled = !state.isSaving) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                                }
                                GlassOptionsMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, glassState = glassState) {
                                    GlassOptionsItem("Editar categoría", GastifyIcons.edit, onClick = { menuExpanded = false; openEditor(category) })
                                    GlassOptionsDivider()
                                    GlassOptionsItem("Eliminar categoría", GastifyIcons.delete, onClick = { menuExpanded = false; toDeleteId = category.id }, destructive = true)
                                }
                            }
                        }
                        if (gridView) {
                            Spacer(Modifier.height(12.dp))
                            Text(category.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(5.dp))
                            Text(if (category.type == "INCOME") "Ingreso" else "Gasto", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight + 64.dp)
                .background(AmoledBlack.copy(alpha = headerAlpha))
                .padding(start = 12.dp, end = 12.dp, top = statusBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(GastifyIcons.back, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                Text("Categorías", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(workspace.name, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = { openEditor(null) },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.08f),
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(GastifyIcons.add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(5.dp))
                Text("Agregar categoría", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // CREAR O EDITAR CATEGORÍA

    if (showEditor) {

        val cleanName = categoryName.trim()

        AlertDialog(
            onDismissRequest = {
                if (!state.isSaving) {
                    showEditor = false
                }
            },
            containerColor = SurfaceDark,
            title = {

                Text(
                    text = if (editingId == null) {
                        "Nueva categoría"
                    } else {
                        "Editar categoría"
                    }
                )
            },
            text = {

                Column(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it.take(60) },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ejemplo: Recarga de tarjeta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))

                    Text("Tipo de movimiento", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = categoryType == CategoryType.EXPENSE.name, onClick = { categoryType = CategoryType.EXPENSE.name; categoryIcon = "cardpay" }, label = { Text("Gasto") })
                        FilterChip(selected = categoryType == CategoryType.INCOME.name, onClick = { categoryType = CategoryType.INCOME.name; categoryIcon = "recharge" }, label = { Text("Ingreso") })
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Acción", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    val actionKeys = if (categoryType == CategoryType.INCOME.name) {
                        listOf("recharge", "money")
                    } else {
                        listOf("cardpay", "bills", "bus", "food", "money", "tap", "health", "home", "receipt", "gaming")
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        actionKeys.forEach { key ->
                            val option = categoryIconOptions.firstOrNull { it.key == key }
                            if (option != null) {
                                FilterChip(
                                    selected = categoryIcon == option.key,
                                    onClick = { categoryIcon = option.key },
                                    label = { Text(option.label) },
                                    leadingIcon = { Icon(GastifyIcons.category(option.key), contentDescription = null, modifier = Modifier.size(20.dp)) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Color", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryColors) { value ->
                            FilterChip(
                                selected = categoryColor == value,
                                onClick = { categoryColor = value },
                                label = { Box(Modifier.size(20.dp).clip(CircleShape).background(categoryTint(value))) }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            },
            confirmButton = {

                TextButton(
                    enabled = cleanName.isNotEmpty() &&
                        !state.isSaving,
                    onClick = {

                        vm.saveCategory(
                            editingId,
                            cleanName,
                            categoryType,
                            categoryIcon,
                            categoryColor
                        )

                        showEditor = false
                    }
                ) {

                    Text(
                        text = "Guardar",
                        color = PrimaryGreen
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showEditor = false
                    },
                    enabled = !state.isSaving
                ) {

                    Text(
                        text = "Cancelar",
                        color = TextSecondary
                    )
                }
            }
        )
    }

    // ELIMINAR CATEGORÍA

    val categoryToDelete = state.categories.firstOrNull {
        it.id == toDeleteId
    }

    if (categoryToDelete != null) {

        AlertDialog(
            onDismissRequest = {
                toDeleteId = null
            },
            containerColor = SurfaceDark,
            title = {
                Text("Eliminar categoría")
            },
            text = {
                Text(
                    text = "¿Seguro que deseas eliminar \"${categoryToDelete.name}\"?",
                    color = TextSecondary
                )
            },
            confirmButton = {

                TextButton(
                    enabled = !state.isSaving,
                    onClick = {
                        vm.deleteCategory(categoryToDelete.id)
                        toDeleteId = null
                    }
                ) {

                    Text(
                        text = "Eliminar",
                        color = ExpenseRed
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        toDeleteId = null
                    }
                ) {

                    Text(
                        text = "Cancelar",
                        color = TextSecondary
                    )
                }
            }
        )
    }
}
