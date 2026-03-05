package com.intimocoffee.waiter.feature.orders.di

import com.intimocoffee.waiter.feature.orders.data.repository.OrderRepositoryImpl
import com.intimocoffee.waiter.feature.orders.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrderModule {

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        orderRepositoryImpl: OrderRepositoryImpl
    ): OrderRepository
}