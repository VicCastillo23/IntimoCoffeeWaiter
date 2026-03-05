package com.intimocoffee.waiter.feature.tables.di

import com.intimocoffee.waiter.feature.tables.data.repository.TableRepositoryImpl
import com.intimocoffee.waiter.feature.tables.domain.repository.TableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TableModule {

    @Binds
    @Singleton
    abstract fun bindTableRepository(
        tableRepositoryImpl: TableRepositoryImpl
    ): TableRepository
}