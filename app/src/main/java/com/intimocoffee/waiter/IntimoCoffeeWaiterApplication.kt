package com.intimocoffee.waiter

import android.app.Application
import android.util.Log
import com.intimocoffee.waiter.core.database.DatabaseInitializer
import com.intimocoffee.waiter.core.network.DynamicRetrofitProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class IntimoCoffeeWaiterApplication : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    @Inject
    lateinit var dynamicRetrofitProvider: DynamicRetrofitProvider

    override fun onCreate() {
        super.onCreate()

        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        appScope.launch {
            databaseInitializer.initializeDatabase()
        }

        // Antes solo se llamaba en login(); la UI principal pide /api/orders sin descubrir → quedaba 10.0.2.2 (emulador).
        appScope.launch {
            try {
                dynamicRetrofitProvider.discoverAndRefreshService()
                Log.i(
                    TAG,
                    "Servidor principal: ${dynamicRetrofitProvider.getCurrentServerUrl()}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "discoverAndRefreshService al iniciar", e)
            }
        }
    }

    companion object {
        private const val TAG = "IntimoCoffeeWaiter"
    }
}
