package com.intimocoffee.waiter.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "reservations",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tableId"])]
)
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tableId: Long,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String?,
    val partySize: Int,
    val reservationDate: String, // ISO-8601 format
    val duration: Int, // in minutes
    val status: String, // ReservationStatus as string
    val notes: String?,
    val isActive: Boolean = true,
    val createdAt: String, // ISO-8601 format
    val updatedAt: String, // ISO-8601 format
    val createdBy: Long
)