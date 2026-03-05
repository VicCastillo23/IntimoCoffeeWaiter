package com.intimocoffee.waiter.feature.settings.presentation.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.model.UserRole
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersManagementScreen(
    viewModel: UsersManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with stats
        Text(
            text = "Gestión de Personal",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats Cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StatsCard(
                    title = "Total Activos",
                    value = uiState.userStats.totalActive.toString(),
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                StatsCard(
                    title = "Gerentes",
                    value = uiState.userStats.managerCount.toString(),
                    icon = Icons.Default.SupervisorAccount,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                StatsCard(
                    title = "Meseros",
                    value = uiState.userStats.waiterCount.toString(),
                    icon = Icons.Default.RestaurantMenu,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            item {
                StatsCard(
                    title = "Baristas",
                    value = uiState.userStats.baristaCount.toString(),
                    icon = Icons.Default.LocalCafe,
                    color = MaterialTheme.colorScheme.surface
                )
            }
            item {
                StatsCard(
                    title = "Cocineros",
                    value = uiState.userStats.cookCount.toString(),
                    icon = Icons.Default.Restaurant,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Empleados (${uiState.filteredUsers.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            FilledTonalButton(
                onClick = { viewModel.showAddUserDialog() }
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nuevo Empleado")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Role Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { viewModel.filterByRole(null) },
                    label = { Text("Todos") },
                    selected = uiState.selectedRole == null
                )
            }
            
            items(UserRole.values()) { role ->
                FilterChip(
                    onClick = { viewModel.filterByRole(role) },
                    label = { Text(role.displayName) },
                    selected = uiState.selectedRole == role,
                    leadingIcon = if (uiState.selectedRole == role) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Users List
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.filteredUsers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.selectedRole != null) {
                                "No hay empleados con el rol ${uiState.selectedRole!!.displayName}"
                            } else {
                                "No hay empleados registrados"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredUsers) { user ->
                        UserCard(
                            user = user,
                            onEditClick = { viewModel.showEditUserDialog(user) },
                            onDeactivateClick = {
                                scope.launch {
                                    viewModel.deactivateUser(user.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add/Edit User Dialog
    if (uiState.showAddUserDialog) {
        AddEditUserDialog(
            editingUser = uiState.editingUser,
            onDismiss = { viewModel.hideUserDialog() },
            onSave = { user ->
                if (uiState.editingUser == null) {
                    // Create new user
                    viewModel.createUser(
                        username = user.username,
                        password = user.password,
                        fullName = user.fullName,
                        email = user.email,
                        phone = user.phone,
                        role = user.role,
                        hireDate = user.hireDate,
                        salary = user.salary
                    )
                } else {
                    // Update existing user
                    viewModel.updateUser(
                        userId = uiState.editingUser!!.id,
                        username = user.username,
                        password = if (user.password.isNotEmpty()) user.password else null,
                        fullName = user.fullName,
                        email = user.email,
                        phone = user.phone,
                        role = user.role,
                        hireDate = user.hireDate,
                        salary = user.salary
                    )
                }
            }
        )
    }
    
    // Show error if exists
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Here you could show a Snackbar
            viewModel.clearError()
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    onEditClick: () -> Unit,
    onDeactivateClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = getRoleColor(user.role).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = user.role.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = getRoleColor(user.role),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (user.email != null) {
                        Text(
                            text = user.email!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    if (user.phone != null) {
                        Text(
                            text = user.phone!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    if (user.salary != null) {
                        Text(
                            text = "Salario: ${currencyFormat.format(user.salary)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onDeactivateClick) {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = "Desactivar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getRoleColor(role: UserRole): Color {
    return when (role) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.error
        UserRole.MANAGER -> MaterialTheme.colorScheme.primary
        UserRole.WAITER -> MaterialTheme.colorScheme.secondary
        UserRole.BARISTA -> MaterialTheme.colorScheme.tertiary
        UserRole.COOK -> MaterialTheme.colorScheme.outline
    }
}