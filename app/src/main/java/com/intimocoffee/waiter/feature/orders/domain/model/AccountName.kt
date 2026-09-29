package com.intimocoffee.waiter.feature.orders.domain.model

/** Nombre usable como etiqueta de cuenta; descarta vacíos, teléfonos y "Para llevar". */
fun displayableAccountName(raw: String?): String? {
    val name = raw?.trim().orEmpty()
    if (name.isEmpty() || name.none { it.isLetter() }) return null
    if (name.equals("Para llevar", ignoreCase = true)) return null
    return name
}
