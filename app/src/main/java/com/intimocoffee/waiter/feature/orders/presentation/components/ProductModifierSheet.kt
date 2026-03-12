package com.intimocoffee.waiter.feature.orders.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.intimocoffee.waiter.core.network.ModifierOptionResponse
import com.intimocoffee.waiter.feature.products.domain.model.Product
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

// ─── Category groups ──────────────────────────────────────────────────────────
private val CALIENTES_IDS    = setOf(1L, 2L, 3L)
private val ESPECIALIDAD_IDS = setOf(4L, 5L, 6L)
private val FRIOS_IDS        = setOf(7L, 8L, 9L, 10L)
private val TISANAS_IDS      = setOf(11L, 12L, 13L)
private val POSTRES_IDS      = setOf(14L)
private val SALADOS_IDS      = setOf(15L, 16L, 17L)

private fun staticMultiFor(catId: Long): List<String> = when (catId) {
    in CALIENTES_IDS    -> listOf("Crema Batida")
    in ESPECIALIDAD_IDS -> emptyList()
    in FRIOS_IDS        -> listOf("Crema Batida")
    in TISANAS_IDS      -> emptyList()
    in POSTRES_IDS      -> listOf(
        "Helado extra", "Crema batida", "Chocolate extra",
        "Mermelada extra", "Miel de maple extra"
    )
    in SALADOS_IDS      -> listOf(
        "Salsa extra", "Sin cebolla", "Sin mayonesa", "Sin picante",
        "Sin queso", "Sin sal", "Con extra queso",
        "Vinagreta extra", "Queso amarillo extra"
    )
    else                -> listOf("Para llevar", "Para aquí")
}

private fun staticSingleFor(catId: Long): List<String>? = when (catId) {
    in TISANAS_IDS -> listOf("Caliente", "Tibia", "Filtrada", "Con frutos")
    else           -> null
}

// ─── Colors by category ───────────────────────────────────────────────────────
private val catColorMap = mapOf(
    1L  to Color(0xFF8D4925),
    2L  to Color(0xFF8D4925),
    3L  to Color(0xFF8D4925),
    4L  to Color(0xFF6D4C41),
    5L  to Color(0xFF455A64),
    6L  to Color(0xFF546E7A),
    7L  to Color(0xFF1E88E5),
    8L  to Color(0xFF1565C0),
    9L  to Color(0xFF0288D1),
    10L to Color(0xFF039BE5),
    11L to Color(0xFF43A047),
    12L to Color(0xFF2E7D32),
    13L to Color(0xFF388E3C),
    14L to Color(0xFFF9A825),
    15L to Color(0xFFFF6F00),
    16L to Color(0xFFE65100),
    17L to Color(0xFFBF360C),
    18L to Color(0xFF757575),
)
private val defaultCatColor = Color(0xFF757575)
private fun catColor(id: Long): Color = catColorMap[id] ?: defaultCatColor

// ─── Main sheet ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductModifierSheet(
    product: Product,
    modifierOptions: Map<Long, List<ModifierOptionResponse>> = emptyMap(),
    onAdd: (modifiers: List<String>, note: String, priceExtra: BigDecimal) -> Unit,
    onDismiss: () -> Unit
) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val catId = product.categoryId
    val color = catColor(catId)
    val dynamicOptions = modifierOptions[catId] ?: emptyList()

    val showDynamicSingle = (catId in ESPECIALIDAD_IDS || catId in FRIOS_IDS) && dynamicOptions.isNotEmpty()
    val showDynamicPrices = catId in FRIOS_IDS
    val dynamicSectionTitle = when (catId) {
        in ESPECIALIDAD_IDS -> "Origen del café"
        in FRIOS_IDS        -> "Tipo de leche"
        else                -> "Opciones"
    }

    val staticSingleOptions = staticSingleFor(catId)
    val staticMultiOptions  = staticMultiFor(catId)

    // State
    val multiSelected   = remember { mutableStateListOf<String>() }
    var staticSingle    by remember { mutableStateOf<String?>(null) }
    var dynamicSelected by remember { mutableStateOf<ModifierOptionResponse?>(null) }
    var customNote      by remember { mutableStateOf("") }

    // Live price display
    val displayPrice = product.price.add(
        if (showDynamicPrices) dynamicSelected?.priceExtra?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        else BigDecimal.ZERO
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // ── Header ──────────────────────────────────────────────────────
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
                        text = fmt.format(displayPrice),
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

            // ── Temperatura (Tisanas/Té) — static single-select ──────────
            staticSingleOptions?.let { temps ->
                SectionHeader(title = "Temperatura", color = color)
                SingleSelectChips(
                    options = temps,
                    selected = staticSingle,
                    onSelect = { staticSingle = if (staticSingle == it) null else it },
                    categoryColor = color,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Origen / Tipo de leche — dynamic single-select ───────────
            if (showDynamicSingle) {
                SectionHeader(title = dynamicSectionTitle, color = color)
                DynamicSingleSelectChips(
                    options = dynamicOptions,
                    selected = dynamicSelected,
                    onSelect = { dynamicSelected = if (dynamicSelected?.id == it.id) null else it },
                    categoryColor = color,
                    showPrices = showDynamicPrices,
                    formatter = fmt,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Modificadores estáticos — multi-select ───────────────────
            if (staticMultiOptions.isNotEmpty()) {
                SectionHeader(title = "Modificadores", color = color)
                FlowChips(
                    modifiers = staticMultiOptions,
                    selected = multiSelected,
                    onToggle = {
                        if (multiSelected.contains(it)) multiSelected.remove(it) else multiSelected.add(it)
                    },
                    categoryColor = color,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Nota libre ───────────────────────────────────────────────
            SectionHeader(title = "Nota adicional", color = color)
            OutlinedTextField(
                value = customNote,
                onValueChange = { if (it.length <= 120) customNote = it },
                placeholder = {
                    Text("Ej: sin azúcar, para llevar\u2026", style = MaterialTheme.typography.bodyMedium)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 2,
                textStyle = MaterialTheme.typography.bodyMedium,
                supportingText = {
                    if (customNote.isNotBlank())
                        Text("${customNote.length}/120", style = MaterialTheme.typography.labelSmall)
                }
            )

            // ── Preview de selección ─────────────────────────────────────
            val allSelected = buildList {
                addAll(multiSelected)
                staticSingle?.let { add(it) }
                dynamicSelected?.let { add(it.name) }
            }
            if (allSelected.isNotEmpty() || customNote.isNotBlank()) {
                val preview = buildString {
                    if (allSelected.isNotEmpty()) append(allSelected.joinToString(", "))
                    if (customNote.isNotBlank()) {
                        if (allSelected.isNotEmpty()) append(" \u2014 ")
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
                        text = "\uD83D\uDCDD $preview",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }

            // ── Botones ──────────────────────────────────────────────────
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
                    onClick = {
                        val finalModifiers = buildList<String> {
                            addAll(multiSelected)
                            staticSingle?.let { add(it) }
                            dynamicSelected?.let { add(it.name) }
                        }.distinct()
                        val priceExtra = if (showDynamicPrices)
                            dynamicSelected?.priceExtra?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        else BigDecimal.ZERO
                        onAdd(finalModifiers, customNote, priceExtra)
                    },
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

// ─── Section header ───────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp)
    )
}

// ─── Static single-select chips ──────────────────────────────────────────────
@Composable
private fun SingleSelectChips(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    categoryColor: Color,
    modifier: Modifier = Modifier
) {
    val rows = options.chunked(4)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { opt ->
                    ModifierChip(
                        label = opt,
                        isSelected = selected == opt,
                        color = categoryColor,
                        onClick = { onSelect(opt) }
                    )
                }
            }
        }
    }
}

// ─── Dynamic single-select chips (with optional price) ───────────────────────
@Composable
private fun DynamicSingleSelectChips(
    options: List<ModifierOptionResponse>,
    selected: ModifierOptionResponse?,
    onSelect: (ModifierOptionResponse) -> Unit,
    categoryColor: Color,
    showPrices: Boolean,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val rows = options.chunked(3)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { opt ->
                    val priceExtra = opt.priceExtra.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val suffix = if (showPrices && priceExtra > BigDecimal.ZERO)
                        " +${formatter.format(priceExtra)}" else ""
                    ModifierChip(
                        label = "${opt.name}$suffix",
                        isSelected = selected?.id == opt.id,
                        color = categoryColor,
                        onClick = { onSelect(opt) }
                    )
                }
            }
        }
    }
}

// ─── Multi-select chips ───────────────────────────────────────────────────────
@Composable
private fun FlowChips(
    modifiers: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    categoryColor: Color,
    modifier: Modifier = Modifier
) {
    val rows = modifiers.chunked(3)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { mod ->
                    ModifierChip(
                        label = mod,
                        isSelected = selected.contains(mod),
                        color = categoryColor,
                        onClick = { onToggle(mod) },
                        showCheckmark = true
                    )
                }
            }
        }
    }
}

// ─── Individual chip ──────────────────────────────────────────────────────────
@Composable
private fun ModifierChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    showCheckmark: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color else color.copy(0.08f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(if (isSelected) 1f else 0.25f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showCheckmark && isSelected) {
                Icon(
                    Icons.Default.Check, null,
                    Modifier.size(13.dp),
                    tint = Color.White
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else color
            )
        }
    }
}
