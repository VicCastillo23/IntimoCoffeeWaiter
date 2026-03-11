package com.intimocoffee.waiter

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.intimocoffee.waiter.core.navigation.Destinations
import com.intimocoffee.waiter.feature.auth.presentation.LoginScreen
import com.intimocoffee.waiter.ui.theme.IntimoCoffeeAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            IntimoCoffeeAppTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { delay(1500); showSplash = false }
                
                if (showSplash) {
                    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.splash_logo), "Íntimo Coffee", Modifier.size(280.dp))
                    }
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        IntimoCoffeeApp()
                    }
                }
            }
        }
    }
}

@Composable
fun IntimoCoffeeApp() {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf(Destinations.LOGIN) }
    
    // Check if user is already logged in
    val mainViewModel: MainViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        if (mainViewModel.isUserLoggedIn()) {
            startDestination = Destinations.MAIN
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Destinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Destinations.MAIN) {
                        popUpTo(Destinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Destinations.MAIN) {
            WaiterMainScreen(
                onNavigateToCreateOrder = {
                    navController.navigate(Destinations.CREATE_ORDER)
                },
                onLogout = {
                    mainViewModel.logout()
                    navController.navigate(Destinations.LOGIN) {
                        popUpTo(Destinations.MAIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Destinations.CREATE_ORDER) {
            com.intimocoffee.waiter.feature.orders.presentation.create.CreateOrderScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}