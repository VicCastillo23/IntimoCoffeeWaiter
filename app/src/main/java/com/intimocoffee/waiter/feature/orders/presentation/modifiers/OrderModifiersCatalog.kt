package com.intimocoffee.waiter.feature.orders.presentation.modifiers

import java.math.BigDecimal

/** Modificador con cargo extra en MXN (menú físico). */
data class PricedModifier(val label: String, val price: BigDecimal)

object OrderModifiersCatalog {

    private fun p(amount: Long) = BigDecimal.valueOf(amount)

    /** Extracciones cortas */
    val extraccionesCortas: List<PricedModifier> = listOf(
        PricedModifier("Crema batida", p(12)),
        PricedModifier("Leche almendra", p(17)),
        PricedModifier("Leche coco", p(17)),
        PricedModifier("Leche soya", p(17)),
        PricedModifier("Leche avena", p(20)),
        PricedModifier("Deslactosada", BigDecimal.ZERO),
        PricedModifier("Descafeinado", BigDecimal.ZERO),
        PricedModifier("Bombones", p(12)),
    )

    /** Café y lattes — jarabes y extras */
    val cafeYLattes: List<PricedModifier> = listOf(
        PricedModifier("Jarabe brown sugar", p(9)),
        PricedModifier("Extra café", p(17)),
        PricedModifier("Tapioca", p(26)),
        PricedModifier("Jarabe de avellana", p(15)),
        PricedModifier("Jarabe de crema irlandesa", p(15)),
        PricedModifier("Jarabe de chocolate", p(15)),
        PricedModifier("Jarabe de menta", p(15)),
        PricedModifier("Jarabe de coco", p(15)),
        PricedModifier("Jarabe de jengibre", p(15)),
        PricedModifier("Jarabe de vainilla", p(15)),
        PricedModifier("Jarabe de cheesecake", p(15)),
        PricedModifier("Jarabe de caramelo", p(15)),
        PricedModifier("Jarabe de calabaza", p(15)),
        PricedModifier("Extra rompope", p(20)),
        PricedModifier("Extra Bailey's", p(20)),
        PricedModifier("Extra RON", p(39)),
        PricedModifier("Perla explosiva", p(19)),
        PricedModifier("Jarabe natural extra", p(15)),
    )

    /** Bebidas frías / preparación */
    val bebidasFrias: List<PricedModifier> = listOf(
        PricedModifier("Sin hielo", BigDecimal.ZERO),
        PricedModifier("Poco hielo", BigDecimal.ZERO),
        PricedModifier("Sin jarabe", BigDecimal.ZERO),
        PricedModifier("Poco jarabe", BigDecimal.ZERO),
        PricedModifier("Para llevar", BigDecimal.ZERO),
        PricedModifier("Vaso 16 oz", p(17)),
        PricedModifier("Sin panna", BigDecimal.ZERO),
        PricedModifier("Dos platos", BigDecimal.ZERO),
        PricedModifier("Frío o frappé (bebidas frías)", BigDecimal.ZERO),
        PricedModifier("Tibio", BigDecimal.ZERO),
    )

    /** Postres — etiquetas únicas (evitar choque con bebidas). */
    val postresExtras: List<PricedModifier> = listOf(
        PricedModifier("Helado extra", BigDecimal.ZERO),
        PricedModifier("Crema batida (postre)", BigDecimal.ZERO),
        PricedModifier("Chocolate extra (postre)", BigDecimal.ZERO),
        PricedModifier("Mermelada extra", BigDecimal.ZERO),
        PricedModifier("Miel de maple extra", BigDecimal.ZERO),
    )

    /** Salados — columna derecha del menú */
    val saladosExtras: List<PricedModifier> = listOf(
        PricedModifier("A la mexicana", p(22)),
        PricedModifier("Chorizo", p(26)),
        PricedModifier("Diezmillo", p(45)),
        PricedModifier("Huevo", p(20)),
        PricedModifier("Jamón", p(20)),
        PricedModifier("Jamón serrano", p(32)),
        PricedModifier("Pollo", p(34)),
        PricedModifier("Salchicha", p(22)),
        PricedModifier("Tocino", p(24)),
    )

    val salsas: List<PricedModifier> = listOf(
        PricedModifier("Salsa BBQ", BigDecimal.ZERO),
        PricedModifier("Salsa Búfalo", BigDecimal.ZERO),
        PricedModifier("Salsa Mango Habanero", BigDecimal.ZERO),
    )

    val sinOpciones: List<PricedModifier> = listOf(
        PricedModifier("NO queso", BigDecimal.ZERO),
        PricedModifier("NO aguacate", BigDecimal.ZERO),
        PricedModifier("NO cebolla", BigDecimal.ZERO),
        PricedModifier("NO jitomate", BigDecimal.ZERO),
    )

    private val allLists: List<List<PricedModifier>> = listOf(
        extraccionesCortas, cafeYLattes, bebidasFrias, postresExtras,
        saladosExtras, salsas, sinOpciones
    )

    private val priceByLabel: Map<String, BigDecimal> =
        allLists.flatten().associate { it.label to it.price }

    fun totalForSelectedLabels(selected: Set<String>): BigDecimal =
        selected.fold(BigDecimal.ZERO) { acc, label ->
            acc.add(priceByLabel[label] ?: BigDecimal.ZERO)
        }

    /**
     * Secciones estáticas con precio según categoría hoja del producto.
     * IDs alineados con [DatabaseInitializer] / carta Intimo.
     */
    fun staticPricedSectionsForLeafCategory(catId: Long): List<Pair<String, List<PricedModifier>>> =
        when {
            catId in 1L..13L -> listOf(
                "Extracciones cortas" to extraccionesCortas,
                "Café y lattes" to cafeYLattes,
                "Bebidas frías" to bebidasFrias,
            )
            catId == 14L -> listOf("Postres" to postresExtras)
            catId in 15L..17L -> listOf(
                "Extras salados" to saladosExtras,
                "Salsas" to salsas,
                "Sin" to sinOpciones,
            )
            else -> emptyList()
        }

    /** Tisanas / té — temperatura (sin cargo). */
    fun temperaturaTisanaOptions(): List<String> =
        listOf("Caliente", "Tibia", "Filtrada", "Con frutos")
}
