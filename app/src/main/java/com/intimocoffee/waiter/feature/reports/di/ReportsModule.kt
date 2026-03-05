package com.intimocoffee.waiter.feature.reports.di

import com.intimocoffee.waiter.feature.reports.data.repository.ReportsRepositoryImpl
import com.intimocoffee.waiter.feature.reports.domain.repository.ReportsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportsModule {

    @Binds
    @Singleton
    abstract fun bindReportsRepository(
        reportsRepositoryImpl: ReportsRepositoryImpl
    ): ReportsRepository
}