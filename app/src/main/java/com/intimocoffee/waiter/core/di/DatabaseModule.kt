package com.intimocoffee.waiter.core.di

import android.content.Context
import com.intimocoffee.waiter.core.database.IntimoCoffeeDatabase
import com.intimocoffee.waiter.core.database.dao.*
import com.intimocoffee.waiter.feature.dashboard.data.repository.DashboardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IntimoCoffeeDatabase {
        return IntimoCoffeeDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideUserDao(database: IntimoCoffeeDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideCategoryDao(database: IntimoCoffeeDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    fun provideProductDao(database: IntimoCoffeeDatabase): ProductDao {
        return database.productDao()
    }
    
    @Provides
    fun provideOrderDao(database: IntimoCoffeeDatabase): OrderDao {
        return database.orderDao()
    }
    
    @Provides
    fun provideTableDao(database: IntimoCoffeeDatabase): TableDao {
        return database.tableDao()
    }
    
    @Provides
    fun provideReservationDao(database: IntimoCoffeeDatabase): ReservationDao {
        return database.reservationDao()
    }
    
    @Provides
    fun provideNotificationDao(database: IntimoCoffeeDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    fun provideFidelityCustomerDao(database: IntimoCoffeeDatabase): com.intimocoffee.waiter.core.database.dao.FidelityCustomerDao {
        return database.fidelityCustomerDao()
    }
    
    @Provides
    @Singleton
    fun provideDashboardRepository(
        tableDao: TableDao,
        orderDao: OrderDao,
        productDao: ProductDao
    ): DashboardRepository {
        return DashboardRepository(tableDao, orderDao, productDao)
    }
}
