package com.intimocoffee.waiter.feature.reservations.di

import com.intimocoffee.waiter.feature.reservations.data.repository.ReservationRepositoryImpl
import com.intimocoffee.waiter.feature.reservations.domain.repository.ReservationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReservationModule {

    @Binds
    @Singleton
    abstract fun bindReservationRepository(
        reservationRepositoryImpl: ReservationRepositoryImpl
    ): ReservationRepository
}