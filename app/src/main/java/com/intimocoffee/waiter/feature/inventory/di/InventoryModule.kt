package com.intimocoffee.waiter.feature.inventory.di

import com.intimocoffee.waiter.feature.inventory.data.repository.InventoryRepositoryImpl
import com.intimocoffee.waiter.feature.inventory.data.service.InventoryServiceImpl
import com.intimocoffee.waiter.feature.inventory.domain.repository.InventoryRepository
import com.intimocoffee.waiter.feature.inventory.domain.service.InventoryService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InventoryModule {

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        inventoryRepositoryImpl: InventoryRepositoryImpl
    ): InventoryRepository
    
    @Binds
    @Singleton
    abstract fun bindInventoryService(
        inventoryServiceImpl: InventoryServiceImpl
    ): InventoryService
}
