package com.intimocoffee.waiter.core.database.dao

import androidx.room.*
import com.intimocoffee.waiter.core.database.entity.ReservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {
    
    @Query("SELECT * FROM reservations WHERE isActive = 1 ORDER BY reservationDate ASC")
    fun getAllActiveReservations(): Flow<List<ReservationEntity>>
    
    @Query("SELECT * FROM reservations ORDER BY reservationDate ASC")
    fun getAllReservations(): Flow<List<ReservationEntity>>
    
    @Query("SELECT * FROM reservations WHERE id = :id")
    suspend fun getReservationById(id: Long): ReservationEntity?
    
    @Query("SELECT * FROM reservations WHERE tableId = :tableId AND isActive = 1 ORDER BY reservationDate ASC")
    fun getReservationsByTable(tableId: Long): Flow<List<ReservationEntity>>
    
    @Query("SELECT * FROM reservations WHERE status = :status AND isActive = 1 ORDER BY reservationDate ASC")
    fun getReservationsByStatus(status: String): Flow<List<ReservationEntity>>
    
    @Query("SELECT * FROM reservations WHERE DATE(reservationDate) = :date AND isActive = 1 ORDER BY reservationDate ASC")
    fun getReservationsByDate(date: String): Flow<List<ReservationEntity>>
    
    @Query("SELECT * FROM reservations WHERE DATE(reservationDate) = DATE('now', 'localtime') AND isActive = 1 ORDER BY reservationDate ASC")
    fun getTodaysReservations(): Flow<List<ReservationEntity>>
    
    @Query("SELECT * FROM reservations WHERE reservationDate >= datetime('now', 'localtime') AND isActive = 1 ORDER BY reservationDate ASC")
    fun getUpcomingReservations(): Flow<List<ReservationEntity>>
    
    @Query("SELECT * FROM reservations WHERE tableId = :tableId AND isActive = 1 AND reservationDate BETWEEN :startTime AND :endTime")
    suspend fun getConflictingReservations(tableId: Long, startTime: String, endTime: String): List<ReservationEntity>
    
    @Insert
    suspend fun insertReservation(reservation: ReservationEntity): Long
    
    @Update
    suspend fun updateReservation(reservation: ReservationEntity)
    
    @Query("UPDATE reservations SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateReservationStatus(id: Long, status: String, updatedAt: String)
    
    @Query("UPDATE reservations SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun cancelReservation(id: Long, updatedAt: String)
    
    @Delete
    suspend fun deleteReservation(reservation: ReservationEntity)
}