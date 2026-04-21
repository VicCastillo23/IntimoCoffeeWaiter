package com.intimocoffee.waiter.feature.orders.presentation.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intimocoffee.waiter.core.network.ModifierOptionResponse
import com.intimocoffee.waiter.feature.products.domain.model.Product
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

// ─── Category groups (hoja) ─────────────────────────────────────────────────
private val ESPECIALIDAD_IDS = setOf(4L, 5L, 6L)
private val FRIOS_IDS = setOf(7L, 8L, 9L, 10L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductModifierSheet(
    product: Product,
    modifierOptions: Map<Long, List<ModifierOptionResponse>> = emptyMap(),
    pricedSections: List<Pair<String, List<ModifierOptionResponse>>> = emptyList(),
    temperaturaOptions: List<ModifierOptionResponse> = emptyList(),
    onAdd: (modifiers: List<String>, note: String, priceExtra: BigDecimal) -> Unit,
    onDismiss: () -> Unit
) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    val catId = product.categoryId
    // Misma paleta que IntimoCoffeeApp (evita chips por categoría de colores distintos).
    val accent = MaterialTheme.colorScheme.primary
    val dynamicOptions = modifierOptions[catId] ?: emptyList()

    val showDynamicSingle = (catId in ESPECIALIDAD_IDS || catId in FRIOS_IDS) && dynamicOptions.isNotEmpty()
    val showDynamicPrices = catId in FRIOS_IDS
    val dynamicSectionTitle = when (catId) {
        in ESPECIALIDAD_IDS -> "Origen del café"
        in FRIOS_IDS -> "Tipo de leche"
        else -> "Opciones"
    }

    val showTemperaturaTisana = temperaturaOptions.isNotEmpty()

    var staticSingle by remember(product.id) { mutableStateOf<String?>(null) }
    var dynamicSelected by remember(product.id) { mutableStateOf<ModifierOptionResponse?>(null) }
    var customNote by remember(product.id) { mutableStateOf("") }
    val selectedPricedIds = remember(product.id) { mutableStateListOf<String>() }

    val pricedFlat = remember(pricedSections) {
        pricedSections.flatMap { it.second }.associateBy { it.id }
    }
    val staticExtra = remember(selectedPricedIds.toList(), pricedSections) {
        selectedPricedIds.fold(BigDecimal.ZERO) { acc, id ->
            acc.add(pricedFlat[id]?.priceExtra?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        }
    }
    val dynamicExtra = if (showDynamicPrices)
        dynamicSelected?.priceExtra?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    else BigDecimal.ZERO

    val displayPrice = product.price.add(staticExtra).add(dynamicExtra)

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accent)
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
                        color = accent
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = "Personalizar",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))

            if (showTemperaturaTisana) {
                SectionHeader(title = "Temperatura")
                SingleSelectChips(
                    options = temperaturaOptions.map { it.name },
                    selected = staticSingle,
                    onSelect = { staticSingle = if (staticSingle == it) null else it },
                    categoryColor = accent,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (showDynamicSingle) {
                SectionHeader(title = dynamicSectionTitle)
                DynamicSingleSelectChips(
                    options = dynamicOptions,
                    selected = dynamicSelected,
                    onSelect = { dynamicSelected = if (dynamicSelected?.id == it.id) null else it },
                    categoryColor = accent,
                    showPrices = showDynamicPrices,
                    formatter = fmt,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            pricedSections.forEach { (title, items) ->
                SectionHeader(title = title)
                PricedMultiChips(
                    items = items,
                    selectedIds = selectedPricedIds,
                    categoryColor = accent,
                    formatter = fmt,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            SectionHeader(title = "Nota adicional")
            OutlinedTextField(
                value = customNote,
                onValueChange = { if (it.length <= 120) customNote = it },
                placeholder = {
                    Text("Ej: sin azúcar, indicaciones extra…", style = MaterialTheme.typography.bodyMedium)
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

            val allSelected = buildList {
                addAll(selectedPricedIds.mapNotNull { pricedFlat[it]?.name })
                staticSingle?.let { add(it) }
                dynamicSelected?.let { add(it.name) }
            }
            if (allSelected.isNotEmpty() || customNote.isNotBlank()) {
                val preview = buildString {
                    if (allSelected.isNotEmpty()) append(allSelected.joinToString(", "))
                    if (customNote.isNotBlank()) {
                        if (allSelected.isNotEmpty()) append(" — ")
                        append(customNote.trim())
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = "📝 $preview",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        val finalModifiers = buildList {
                            addAll(selectedPricedIds.mapNotNull { pricedFlat[it]?.name })
                            staticSingle?.let { add(it) }
                            dynamicSelected?.let { add(it.name) }
                        }.distinct()
                        val priceExtra = staticExtra.add(dynamicExtra)
                        onAdd(finalModifiers, customNote, priceExtra)
                    },
                    modifier = Modifier.weight(2f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar al carrito", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp)
    )
}

@Composable
private fun SingleSelectChips(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    categoryColor: Color,
    modifier: Modifier = Modifier
) {
    val cols = 2
    val rows = options.chunked(cols)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { opt ->
                    Box(modifier = Modifier.weight(1f)) {
                        ModifierChip(
                            label = opt,
                            isSelected = selected == opt,
                            color = categoryColor,
                            onClick = { onSelect(opt) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                repeat(cols - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

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
    val cols = 2
    val rows = options.chunked(cols)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { opt ->
                    val priceExtra = opt.priceExtra.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val suffix = if (showPrices && priceExtra > BigDecimal.ZERO)
                        " +${formatter.format(priceExtra)}" else ""
                    Box(modifier = Modifier.weight(1f)) {
                        ModifierChip(
                            label = "${opt.name}$suffix",
                            isSelected = selected?.id == opt.id,
                            color = categoryColor,
                            onClick = { onSelect(opt) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                repeat(cols - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PricedMultiChips(
    items: List<ModifierOptionResponse>,
    selectedIds: MutableList<String>,
    categoryColor: Color,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val cols = 2
    val rows = items.chunked(cols)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { opt ->
                    val price = opt.priceExtra.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val suffix = if (price > BigDecimal.ZERO) " +${formatter.format(price)}" else ""
                    val label = "${opt.name}$suffix"
                    val isOn = selectedIds.contains(opt.id)
                    Box(modifier = Modifier.weight(1f)) {
                        ModifierChip(
                            label = label,
                            isSelected = isOn,
                            color = categoryColor,
                            onClick = {
                                if (isOn) selectedIds.remove(opt.id) else selectedIds.add(opt.id)
                            },
                            showCheckmark = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                repeat(cols - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModifierChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    showCheckmark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val onSurface = scheme.onSurface
    val bg = if (isSelected) color else scheme.surfaceVariant.copy(alpha = 0.65f)
    val fg = if (isSelected) scheme.onPrimary else onSurface
    val borderCol = if (isSelected) color else scheme.outline.copy(alpha = 0.45f)
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showCheckmark && isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.onPrimary,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = fg,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
