package com.intimocoffee.waiter.feature.orders.presentation.modifiers

import com.intimocoffee.waiter.core.network.ModifierOptionResponse

private const val DYNAMIC = "DYNAMIC"
private const val PRICED_MULTI = "PRICED_MULTI"
private const val TEMP_SINGLE = "TEMP_SINGLE"

fun splitModifierOptionsFromApi(
    list: List<ModifierOptionResponse>,
): Triple<
    Map<Long, List<ModifierOptionResponse>>,
    Map<Long, List<Pair<String, List<ModifierOptionResponse>>>>,
    Map<Long, List<ModifierOptionResponse>>,
    > {
    val active = list.filter { it.isActive }

    val dynamic = active
        .filter { it.uiGroup == DYNAMIC }
        .groupBy { it.categoryId.toLongOrNull() ?: 0L }
        .mapValues { (_, opts) -> opts.sortedBy { it.sortOrder } }

    val priced = active
        .filter { it.uiGroup == PRICED_MULTI }
        .groupBy { it.categoryId.toLongOrNull() ?: 0L }
        .mapValues { (_, opts) ->
            opts.groupBy { it.sectionTitle ?: "" }
                .entries
                .sortedBy { (_, rows) -> rows.minOfOrNull { it.sectionSortOrder } ?: 0 }
                .map { (title, rows) ->
                    title to rows.sortedBy { it.sortOrder }
                }
        }

    val temp = active
        .filter { it.uiGroup == TEMP_SINGLE }
        .groupBy { it.categoryId.toLongOrNull() ?: 0L }
        .mapValues { (_, opts) -> opts.sortedBy { it.sortOrder } }

    return Triple(dynamic, priced, temp)
}
