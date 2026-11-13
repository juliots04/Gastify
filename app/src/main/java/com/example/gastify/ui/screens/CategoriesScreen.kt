package com.example.gastify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.model.Category
import com.example.gastify.data.model.CategoryType
import com.example.gastify.data.model.Workspace
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.components.categoryIconOptions
import com.example.gastify.ui.theme.*
import com.example.gastify.viewmodel.CategoryViewModel

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

    val visible = state.categories.filter {
        filter == "ALL" || it.type == filter
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = onBack
                ) {

                    Icon(
                        imageVector = GastifyIcons.back,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text("Volver")
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Categorías",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = workspace.name,
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(26.dp))

                Button(
                    onClick = {
                        openEditor(null)
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = AmoledBlack
                    ),
                    contentPadding = PaddingValues(16.dp)
                ) {

                    Icon(
                        imageVector = GastifyIcons.add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Nueva categoría",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Mis categorías",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    FilterChip(
                        selected = filter == "ALL",
                        onClick = {
                            filter = "ALL"
                        },
                        label = {
                            Text("Todas")
                        }
                    )

                    FilterChip(
                        selected = filter == "EXPENSE",
                        onClick = {
                            filter = "EXPENSE"
                        },
                        label = {
                            Text("Gastos")
                        }
                    )

                    FilterChip(
                        selected = filter == "INCOME",
                        onClick = {
                            filter = "INCOME"
                        },
                        label = {
                            Text("Ingresos")
                        }
                    )
                }
            }

            if (state.isLoading) {

                item {

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

                item {

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

                items(
                    items = visible,
                    key = { it.id }
                ) { category ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Surface(
                                    color = CardDark,
                                    shape = RoundedCornerShape(14.dp)
                                ) {

                                    Box(
                                        modifier = Modifier.size(52.dp),
                                        contentAlignment = Alignment.Center
                                    ) {

                                        Icon(
                                            imageVector = GastifyIcons.category(
                                                category.icon
                                            ),
                                            contentDescription = null,
                                            tint = categoryTint(
                                                category.color
                                            ),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = category.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(Modifier.height(5.dp))

                                    Text(
                                        text = if (
                                            category.type == "INCOME"
                                        ) {
                                            "Ingreso"
                                        } else {
                                            "Gasto"
                                        },
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            categoryTint(category.color)
                                        )
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            HorizontalDivider(
                                color = BorderDark
                            )

                            Row {

                                TextButton(
                                    onClick = {
                                        openEditor(category)
                                    },
                                    enabled = !state.isSaving
                                ) {

                                    Icon(
                                        imageVector = GastifyIcons.edit,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(Modifier.width(5.dp))

                                    Text(
                                        text = "Editar",
                                        color = PrimaryGreen
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        toDeleteId = category.id
                                    },
                                    enabled = !state.isSaving
                                ) {

                                    Icon(
                                        imageVector = GastifyIcons.delete,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(Modifier.width(5.dp))

                                    Text(
                                        text = "Eliminar",
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
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
                        onValueChange = {
                            categoryName = it.take(60)
                        },
                        label = {
                            Text("Nombre")
                        },
                        placeholder = {
                            Text("Ejemplo: Corredor Morado")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Tipo",
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        FilterChip(
                            selected = categoryType == "EXPENSE",
                            onClick = {
                                categoryType = "EXPENSE"
                            },
                            label = {
                                Text("Gasto")
                            }
                        )

                        FilterChip(
                            selected = categoryType == "INCOME",
                            onClick = {
                                categoryType = "INCOME"
                            },
                            label = {
                                Text("Ingreso")
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Icono vectorial",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        items(categoryIconOptions) { option ->

                            FilterChip(
                                selected = categoryIcon == option.key,
                                onClick = {
                                    categoryIcon = option.key
                                },
                                label = {
                                    Text(option.label)
                                },
                                leadingIcon = {

                                    Icon(
                                        imageVector = GastifyIcons.category(
                                            option.key
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Color",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        items(categoryColors) { value ->

                            FilterChip(
                                selected = categoryColor == value,
                                onClick = {
                                    categoryColor = value
                                },
                                label = {

                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                categoryTint(value)
                                            )
                                    )
                                }
                            )
                        }
                    }
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
