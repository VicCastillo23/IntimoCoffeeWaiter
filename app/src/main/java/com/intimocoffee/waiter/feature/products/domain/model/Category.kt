package com.intimocoffee.waiter.feature.products.domain.model

data class Category(
    val id: Long,
    val name: String,
    val description: String?,
    val color: String,
    val icon: String?,
    val isActive: Boolean,
    val sortOrder: Int
) {
    companion object {
        fun getDefaultCategories(): List<Category> = listOf(
            Category(1L, "Bebidas Calientes", "Café, té y otras bebidas calientes", "#8D4925", "☕", true, 1),
            Category(2L, "Bebidas Frías", "Jugos, refrescos y bebidas frías", "#2196F3", "🧊", true, 2),
            Category(3L, "Repostería", "Pasteles, galletas y postres", "#FF9800", "🧁", true, 3),
            Category(4L, "Snacks", "Aperitivos y comidas ligeras", "#4CAF50", "🥨", true, 4),
            Category(5L, "Desayunos", "Opciones para el desayuno", "#FFC107", "🍳", true, 5)
        )
    }
}