package com.intimocoffee.waiter.feature.auth.domain.model

data class User(
    val id: Long = 0L,
    val username: String,
    val password: String = "", // For creation/update only
    val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val role: UserRole,
    val isActive: Boolean = true,
    val hireDate: String? = null,
    val salary: Double? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

enum class UserRole(val displayName: String, val description: String) {
    ADMIN("Administrador", "Acceso completo al sistema"),
    MANAGER("Gerente", "Gestión de operaciones, reportes y supervisión"),
    WAITER("Mesero", "Atención al cliente y toma de pedidos"),
    BARISTA("Barista", "Preparación de bebidas y atención de barra"),
    COOK("Cocinero", "Preparación de alimentos en cocina");
    
    fun hasAdminAccess(): Boolean = this == ADMIN
    fun hasManagerAccess(): Boolean = this in listOf(ADMIN, MANAGER)
    fun canTakeOrders(): Boolean = this in listOf(ADMIN, MANAGER, WAITER)
    fun canPrepareDrinks(): Boolean = this in listOf(ADMIN, MANAGER, BARISTA)
    fun canPrepareFood(): Boolean = this in listOf(ADMIN, MANAGER, COOK)
    fun canManageInventory(): Boolean = this in listOf(ADMIN, MANAGER)
    fun canViewReports(): Boolean = this in listOf(ADMIN, MANAGER)
}
