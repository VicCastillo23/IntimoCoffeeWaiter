package com.intimocoffee.waiter.feature.products.domain.model

/** Categoría hoja (subcategoría) — tiene un parentCategoryId que apunta a un ParentCategory */
data class Category(
    val id: Long,
    val name: String,
    val description: String?,
    val color: String,
    val icon: String?,
    val isActive: Boolean,
    val sortOrder: Int,
    val parentCategoryId: String? = null
) {
    companion object {
        fun getDefaultCategories(): List<Category> = emptyList()
    }
}

/** Categoría padre (nivel superior) — no tiene productos directos, agrupa subcategorías */
data class ParentCategory(
    val id: String,
    val name: String,
    val color: String,
    val sortOrder: Int
)
