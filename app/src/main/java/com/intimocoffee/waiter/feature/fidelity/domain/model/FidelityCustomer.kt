package com.intimocoffee.waiter.feature.fidelity.domain.model

data class FidelityCustomer(
    val id: Long,
    val phone: String,
    val name: String,
    val totalPoints: Int
) {
    val displayName: String
        get() = name.ifBlank { phone }
}
