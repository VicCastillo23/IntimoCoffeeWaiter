package com.intimocoffee.waiter.core.util

/**
 * Parses payloads from the loyalty app QR, e.g. `INTIMO_LOYALTY:+5255...` or legacy `INTIMO_LOYALTY:123`.
 */
sealed class LoyaltyQrParseResult {
    data class ByPhone(val phoneDigits: String) : LoyaltyQrParseResult()
    data class ByCustomerId(val id: Long) : LoyaltyQrParseResult()
}

object LoyaltyQrParser {
    private const val PREFIX = "INTIMO_LOYALTY:"

    fun parse(raw: String): LoyaltyQrParseResult? {
        val t = raw.trim()
        val body = if (t.startsWith(PREFIX, ignoreCase = true)) {
            t.substring(PREFIX.length).trim()
        } else {
            t
        }
        if (body.isBlank()) return null
        val digits = body.filter { it.isDigit() }
        if (digits.length >= 10) return LoyaltyQrParseResult.ByPhone(digits)
        if (digits.isNotEmpty()) {
            val id = digits.toLongOrNull() ?: return null
            return LoyaltyQrParseResult.ByCustomerId(id)
        }
        return null
    }
}
