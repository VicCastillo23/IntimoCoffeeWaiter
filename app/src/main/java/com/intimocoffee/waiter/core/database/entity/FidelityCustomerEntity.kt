package com.intimocoffee.waiter.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fidelity_customers",
    indices = [Index(value = ["phone"], unique = true)]
)
data class FidelityCustomerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "phone") val phone: String,
    @ColumnInfo(name = "name") val name: String = "",
    @ColumnInfo(name = "totalPoints") val totalPoints: Int = 0,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis()
)
