package com.intimocoffee.waiter.feature.fidelity.di

import com.intimocoffee.waiter.feature.fidelity.data.repository.FidelityRepositoryImpl
import com.intimocoffee.waiter.feature.fidelity.domain.repository.FidelityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FidelityModule {

    @Binds
    @Singleton
    abstract fun bindFidelityRepository(impl: FidelityRepositoryImpl): FidelityRepository
}
