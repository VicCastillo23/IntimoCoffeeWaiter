package com.intimocoffee.waiter.feature.products.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class Product(
    val id: Long,
    /** ID real en BD (numérico o UUID). */
    val rawId: String = id.toString(),
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val categoryId: Long,
    val categoryName: String? = null,
    val imageUrl: String?,
    val isActive: Boolean,
    val stockQuantity: Int?,
    val minStockLevel: Int?,
    val barcode: String?,
    /** Porcentaje de IVA (ej. 16). null o 0 = sin impuesto en el precio. */
    val taxRatePercent: BigDecimal? = null
) {
    /** Clave estable para líneas de carrito (evita colisiones cuando [id] es 0 con UUID). */
    fun cartLineKey(): String = rawId.ifBlank { id.toString() }

    val isInStock: Boolean
        get() = stockQuantity?.let { it > 0 } ?: true
    
    val isLowStock: Boolean
        get() = stockQuantity?.let { stock ->
            minStockLevel?.let { min -> stock <= min } ?: false
        } ?: false

    /** Precio de menú / subtotal de línea como total con IVA incluido cuando [taxRatePercent] > 0. */
    fun netFromTaxInclusiveLineTotal(lineTotalTaxInclusive: BigDecimal): BigDecimal {
        val rate = taxRatePercent ?: return lineTotalTaxInclusive
        if (rate.compareTo(BigDecimal.ZERO) == 0) return lineTotalTaxInclusive
        val divisor = BigDecimal.ONE.add(
            rate.divide(BigDecimal(100), 10, RoundingMode.HALF_UP)
        )
        return lineTotalTaxInclusive.divide(divisor, 2, RoundingMode.HALF_UP)
    }

    fun taxIncludedInLineTotal(lineTotalTaxInclusive: BigDecimal): BigDecimal {
        val rate = taxRatePercent ?: return BigDecimal.ZERO
        if (rate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return lineTotalTaxInclusive.subtract(netFromTaxInclusiveLineTotal(lineTotalTaxInclusive))
            .setScale(2, RoundingMode.HALF_UP)
    }
}