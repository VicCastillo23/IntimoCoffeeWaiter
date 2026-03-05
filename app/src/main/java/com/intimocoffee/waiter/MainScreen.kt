package com.intimocoffee.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.waiter.feature.orders.presentation.OrdersScreen
import com.intimocoffee.waiter.feature.tables.presentation.TablesScreen
import com.intimocoffee.waiter.feature.products.presentation.ProductsScreen
import com.intimocoffee.waiter.feature.reservations.presentation.ReservationsScreen
import com.intimocoffee.waiter.feature.dashboard.presentation.DashboardContent
import com.intimocoffee.waiter.R
import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.reports.presentation.DailyCutReportDialog
import com.intimocoffee.waiter.feature.reports.presentation.DailyCutConfirmationDialog
import com.intimocoffee.waiter.feature.inventory.presentation.InventoryScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToCreateOrder: () -> Unit = {},
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
        viewModel.loadDashboardData()
    }
    
    if (uiState.shouldLogout) {
        LaunchedEffect(Unit) {
            onLogout()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                    uiState.currentUser?.let { user ->
                        Text(
                            text = "- ${user.fullName} (${user.role.name})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = viewModel::logout) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = stringResource(id = R.string.logout)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        // Main Content
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Sidebar Navigation
            NavigationRail(
                modifier = Modifier.width(120.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val navigationItems = listOf(
                    NavigationItem(
                        icon = Icons.Default.PointOfSale,
                        label = "POS",
                        route = "pos"
                    ),
                    NavigationItem(
                        icon = Icons.Default.Receipt,
                        label = "Órdenes",
                        route = "orders"
                    ),
                    NavigationItem(
                        icon = Icons.Default.Inventory,
                        label = "Productos",
                        route = "products"
                    ),
                    NavigationItem(
                        icon = Icons.Default.Assessment,
                        label = "Stock",
                        route = "inventory"
                    ),
                    NavigationItem(
                        icon = Icons.Default.TableRestaurant,
                        label = "Mesas",
                        route = "tables"
                    ),
                    NavigationItem(
                        icon = Icons.Default.EventAvailable,
                        label = "Reservas",
                        route = "reservations"
                    ),
                    NavigationItem(
                        icon = Icons.Default.Analytics,
                        label = "Reportes",
                        route = "reports"
                    ),
                    NavigationItem(
                        icon = Icons.Default.Settings,
                        label = "Config.",
                        route = "settings"
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                navigationItems.forEach { item ->
                    NavigationRailItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { 
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = uiState.selectedNavItem == item.route,
                        onClick = { viewModel.selectNavItem(item.route) }
                    )
                }
            }
            
            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (uiState.selectedNavItem) {
                    "pos" -> {
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            uiState.dashboardStats?.let { stats ->
                                DashboardContent(
                                    dashboardStats = stats,
                                    todaysReservations = uiState.todaysReservations,
                                    onNavigateToSection = { section -> viewModel.selectNavItem(section) },
                                    onDailyCut = viewModel::showDailyCutConfirmationDialog
                                )
                            } ?: POSContent(
                                onNavigateToCreateOrder = onNavigateToCreateOrder,
                                onDailyCut = viewModel::showDailyCutConfirmationDialog
                            )
                        }
                    }
                    "orders" -> OrdersScreen(onNavigateToCreateOrder = onNavigateToCreateOrder)
                    "products" -> ProductsScreen()
                    "inventory" -> InventoryScreen()
                    "tables" -> TablesScreen()
                    "reservations" -> ReservationsScreen()
                    "reports" -> ReportsContent()
                    "settings" -> SettingsContent()
                    else -> {
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            uiState.dashboardStats?.let { stats ->
                                DashboardContent(
                                    dashboardStats = stats,
                                    todaysReservations = uiState.todaysReservations,
                                    onNavigateToSection = { section -> viewModel.selectNavItem(section) },
                                    onDailyCut = viewModel::showDailyCutConfirmationDialog
                                )
                            } ?: POSContent(
                                onNavigateToCreateOrder = onNavigateToCreateOrder,
                                onDailyCut = viewModel::showDailyCutConfirmationDialog
                            )
                        }
                    }
                }
            }
        }
        
        // Daily Cut Confirmation Dialog
        if (uiState.showDailyCutConfirmationDialog) {
            DailyCutConfirmationDialog(
                onConfirm = viewModel::showDailyCutDialog,
                onDismiss = viewModel::hideDailyCutConfirmationDialog
            )
        }
        
        // Daily Cut Report Dialog
        if (uiState.showDailyCutDialog) {
            DailyCutReportDialog(
                onDismiss = viewModel::hideDailyCutDialog
            )
        }
    }
}

@Composable
fun POSContent(
    onNavigateToCreateOrder: () -> Unit = {},
    onDailyCut: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Coffee,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sistema POS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pantalla principal del punto de venta",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Quick Action Buttons
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(getQuickActions()) { action ->
                    QuickActionButton(
                        icon = action.icon,
                        label = action.label,
                        onClick = { 
                            when (action.label) {
                                "Nueva Orden" -> onNavigateToCreateOrder()
                                "Corte Diario" -> onDailyCut()
                                else -> { /* TODO: Handle other actions */ }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun getQuickActions(): List<QuickAction> = listOf(
    QuickAction(Icons.Default.Add, "Nueva Orden"),
    QuickAction(Icons.Default.Search, "Buscar Producto"),
    QuickAction(Icons.Default.TableRestaurant, "Ver Mesas"),
    QuickAction(Icons.Default.Receipt, "Historial"),
    QuickAction(Icons.Default.Assessment, "Corte Diario")
)

// Placeholder content for other sections
@Composable
fun ReportsContent() {
    PlaceholderContent("Reportes", Icons.Default.Analytics)
}

@Composable
fun SettingsContent() {
    var selectedSettingsTab by remember { mutableStateOf("main") }
    
    when (selectedSettingsTab) {
        "main" -> SettingsMainContent(
            onNavigateToUsers = { selectedSettingsTab = "users" },
            onNavigateBack = { selectedSettingsTab = "main" }
        )
        "users" -> {
            Column {
                // Back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(
                        onClick = { selectedSettingsTab = "main" }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Volver a Configuración")
                    }
                }
                
                com.intimocoffee.waiter.feature.settings.presentation.users.UsersManagementScreen()
            }
        }
    }
}

@Composable
fun SettingsMainContent(
    onNavigateToUsers: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(getSettingsOptions()) { option ->
                    SettingsOptionCard(
                        icon = option.icon,
                        title = option.title,
                        description = option.description,
                        onClick = {
                            when (option.route) {
                                "users" -> onNavigateToUsers()
                                // TODO: Add other settings routes
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

fun getSettingsOptions(): List<SettingsOption> = listOf(
    SettingsOption(
        icon = Icons.Default.People,
        title = "Personal",
        description = "Gestionar empleados, roles y permisos",
        route = "users"
    ),
    SettingsOption(
        icon = Icons.Default.Store,
        title = "Empresa",
        description = "Información de la empresa y configuración",
        route = "company"
    ),
    SettingsOption(
        icon = Icons.Default.Receipt,
        title = "Facturación",
        description = "Configuración de facturación e impuestos",
        route = "billing"
    ),
    SettingsOption(
        icon = Icons.Default.Print,
        title = "Impresoras",
        description = "Configurar impresoras de tickets",
        route = "printers"
    ),
    SettingsOption(
        icon = Icons.Default.Notifications,
        title = "Notificaciones",
        description = "Alertas y notificaciones del sistema",
        route = "notifications"
    ),
    SettingsOption(
        icon = Icons.Default.Backup,
        title = "Respaldo",
        description = "Respaldo y restauración de datos",
        route = "backup"
    )
)

data class SettingsOption(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val route: String
)

@Composable
fun PlaceholderContent(title: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Próximamente disponible",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class NavigationItem(
    val icon: ImageVector,
    val label: String,
    val route: String
)

data class QuickAction(
    val icon: ImageVector,
    val label: String
)
