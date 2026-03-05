package com.intimocoffee.waiter

import android.app.Application
import com.intimocoffee.waiter.core.database.DatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class IntimoCoffeeWaiterApplication : Application() {
    
    @Inject
    lateinit var databaseInitializer: DatabaseInitializer
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database with sample data
        CoroutineScope(Dispatchers.IO).launch {
            databaseInitializer.initializeDatabase()
        }
    }
}
