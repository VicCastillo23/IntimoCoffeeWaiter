package com.intimocoffee.waiter.feature.tables.domain.model

data class Table(
    val id: Long,
    val number: Int,
    val name: String?,
    val capacity: Int,
    val zone: String,
    val status: TableStatus,
    val currentOrderId: Long?,
    val positionX: Float,
    val positionY: Float,
    val isActive: Boolean
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: "Mesa $number"
    
    val isAvailable: Boolean
        get() = status == TableStatus.FREE && isActive
}

enum class TableStatus {
    FREE,
    OCCUPIED,
    RESERVED,
    OUT_OF_SERVICE;
    
    fun getDisplayName(): String {
        return when (this) {
            FREE -> "Libre"
            OCCUPIED -> "Ocupada"
            RESERVED -> "Reservada"
            OUT_OF_SERVICE -> "Fuera de Servicio"
        }
    }
    
    fun getDisplayColor(): String {
        return when (this) {
            FREE -> "#4CAF50" // Green
            OCCUPIED -> "#F44336" // Red
            RESERVED -> "#FF9800" // Orange
            OUT_OF_SERVICE -> "#9E9E9E" // Gray
        }
    }
}