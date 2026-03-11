package com.intimocoffee.waiter.feature.orders.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.intimocoffee.waiter.feature.products.domain.model.Product
import java.text.NumberFormat
import java.util.*

// ─── Modificadores predefinidos por categoría ─────────────────────────────────

private val modifiersByCategory = mapOf(
    1L to listOf(                             // Bebidas calientes / café
        "Leche deslactosada", "Avena", "Leche entera",
        "Sin leche", "Doble shot", "Simple shot",
        "Sin azúcar", "Poca azúcar", "Extra azúcar",
        "Frío", "Tibio", "Sin crema"
    ),
    2L to listOf(                             // Bebidas frías / jugos
        "Sin hielo", "Poco hielo", "Extra hielo",
        "Sin azúcar", "Extra dulce", "Sin gas",
        "Endulzante", "Con leche", "Sin leche"
    ),
    3L to listOf(                             // Comidas / platos
        "Sin cebolla", "Sin crema", "Sin sal",
        "Aparte", "Extra salsa", "Sin picante",
        "Bien cocido", "Término medio", "Poco cocido",
        "Sin queso", "Extra queso"
    ),
    4L to listOf(                             // Otro tipo comida
        "Sin cebolla", "Sin crema", "Sin sal",
        "Aparte", "Extra salsa", "Sin picante",
        "Sin queso", "Extra queso"
    ),
    5L to listOf(                             // Postres
        "Sin crema", "Tibio", "Extra dulce",
        "Sin almíbar", "Extra salsa", "Sin nueces"
    )
)

private val generalModifiers = listOf(
    "Para llevar", "Para aquí", "Sin X", "Extra", "Aparte"
)

private fun getModifiers(categoryId: Long): List<String> =
    (modifiersByCategory[categoryId] ?: generalModifiers)

// ─── Colores por categoría (misma lógica que en CreateOrderScreen) ────────────
private val catColorMap = mapOf(
    1L to Color(0xFF8D4925),
    2L to Color(0xFF1E88E5),
    3L to Color(0xFFFF6F00),
    4L to Color(0xFF43A047),
    5L to Color(0xFFF9A825),
)
private val defaultColor = Color(0xFF757575)
private fun catColor(id: Long): Color = catColorMap[id] ?: defaultColor

// ─── Sheet principal ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductModifierSheet(
    product: Product,
    onAdd: (modifiers: List<String>, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val color = catColor(product.categoryId)
    val modifiers = getModifiers(product.categoryId)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = remember { mutableStateListOf<String>() }
    var customNote by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // ── Header del producto ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(color)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = fmt.format(product.price),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color.copy(0.10f)
                ) {
                    Text(
                        text = "Personalizar",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))

            // ── Modificadores predefinidos ───────────────────────────────────
            Text(
                text = "Modificadores",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp)
            )
            FlowChips(
                modifiers = modifiers,
                selected = selected,
                onToggle = { mod ->
                    if (selected.contains(mod)) selected.remove(mod) else selected.add(mod)
                },
                categoryColor = color,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // ── Nota libre ──────────────────────────────────────────────────────────────────
            Text(
                text = "Nota adicional",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = customNote,
                onValueChange = { if (it.length <= 120) customNote = it },
                placeholder = { Text("Ej: sin azúcar, para llevar…", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 2,
                textStyle = MaterialTheme.typography.bodyMedium,
                supportingText = {
                    if (customNote.isNotBlank()) Text("${customNote.length}/120",
                        style = MaterialTheme.typography.labelSmall)
                }
            )

            // ── Resumen de modificadores seleccionados ───────────────────────
            if (selected.isNotEmpty() || customNote.isNotBlank()) {
                val preview = buildString {
                    if (selected.isNotEmpty()) append(selected.toList().joinToString(", "))
                    if (customNote.isNotBlank()) {
                        if (selected.isNotEmpty()) append(" — ")
                        append(customNote.trim())
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = color.copy(0.08f)
                ) {
                    Text(
                        text = "📝 $preview",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }

            // ── Botones ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onAdd(selected.toList().distinct(), customNote) },
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar al carrito", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Chips de modificadores en flow layout ───────────────────────────────────

@Composable
private fun FlowChips(
    modifiers: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    categoryColor: Color,
    modifier: Modifier = Modifier
) {
    val rows = remember(modifiers) { modifiers.chunked(4) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { mod ->
                    val isSelected = selected.contains(mod)
                    Surface(
                        onClick = { onToggle(mod) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) categoryColor else categoryColor.copy(0.08f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            categoryColor.copy(if (isSelected) 1f else 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check, null,
                                    Modifier.size(13.dp),
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = mod,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else categoryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

