package com.intimocoffee.waiter.feature.tables.data.mapper

import com.intimocoffee.waiter.core.database.entity.TableEntity
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TableMapper @Inject constructor() {
    
    fun toDomain(entity: TableEntity): Table {
        return Table(
            id = entity.id.toLongOrNull() ?: 0L,
            number = entity.number,
            name = entity.name,
            capacity = entity.capacity,
            zone = entity.zone,
            status = TableStatus.valueOf(entity.status),
            currentOrderId = entity.currentOrderId?.toLongOrNull(),
            positionX = entity.positionX,
            positionY = entity.positionY,
            isActive = entity.isActive
        )
    }
    
    fun toEntity(table: Table): TableEntity {
        return TableEntity(
            id = if (table.id == 0L) generateId() else table.id.toString(),
            number = table.number,
            name = table.name,
            capacity = table.capacity,
            zone = table.zone,
            status = table.status.name,
            currentOrderId = table.currentOrderId?.toString(),
            positionX = table.positionX,
            positionY = table.positionY,
            isActive = table.isActive
        )
    }
    
    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}