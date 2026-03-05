package com.intimocoffee.waiter.feature.settings.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.model.UserRole
import com.intimocoffee.waiter.feature.auth.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UsersManagementUiState(
    val users: List<User> = emptyList(),
    val filteredUsers: List<User> = emptyList(),
    val selectedRole: UserRole? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddUserDialog: Boolean = false,
    val editingUser: User? = null,
    val userStats: UserStats = UserStats()
)

data class UserStats(
    val totalActive: Int = 0,
    val adminCount: Int = 0,
    val managerCount: Int = 0,
    val waiterCount: Int = 0,
    val baristaCount: Int = 0,
    val cookCount: Int = 0
)

@HiltViewModel
class UsersManagementViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UsersManagementUiState())
    val uiState: StateFlow<UsersManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadUsers()
        loadUserStats()
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            userRepository.getAllActiveUsers().collect { users ->
                _uiState.value = _uiState.value.copy(
                    users = users,
                    filteredUsers = filterUsers(users, _uiState.value.selectedRole),
                    isLoading = false
                )
            }
        }
    }
    
    private fun loadUserStats() {
        viewModelScope.launch {
            try {
                val totalActive = userRepository.getActiveUsersCount()
                val adminCount = userRepository.getUsersCountByRole(UserRole.ADMIN)
                val managerCount = userRepository.getUsersCountByRole(UserRole.MANAGER)
                val waiterCount = userRepository.getUsersCountByRole(UserRole.WAITER)
                val baristaCount = userRepository.getUsersCountByRole(UserRole.BARISTA)
                val cookCount = userRepository.getUsersCountByRole(UserRole.COOK)
                
                val stats = UserStats(
                    totalActive = totalActive,
                    adminCount = adminCount,
                    managerCount = managerCount,
                    waiterCount = waiterCount,
                    baristaCount = baristaCount,
                    cookCount = cookCount
                )
                
                _uiState.value = _uiState.value.copy(userStats = stats)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error loading user statistics: ${e.message}"
                )
            }
        }
    }
    
    fun filterByRole(role: UserRole?) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            selectedRole = role,
            filteredUsers = filterUsers(currentState.users, role)
        )
    }
    
    private fun filterUsers(users: List<User>, role: UserRole?): List<User> {
        return if (role == null) {
            users
        } else {
            users.filter { it.role == role }
        }
    }
    
    fun showAddUserDialog() {
        _uiState.value = _uiState.value.copy(
            showAddUserDialog = true,
            editingUser = null
        )
    }
    
    fun showEditUserDialog(user: User) {
        _uiState.value = _uiState.value.copy(
            showAddUserDialog = true,
            editingUser = user
        )
    }
    
    fun hideUserDialog() {
        _uiState.value = _uiState.value.copy(
            showAddUserDialog = false,
            editingUser = null
        )
    }
    
    fun createUser(
        username: String,
        password: String,
        fullName: String,
        email: String?,
        phone: String?,
        role: UserRole,
        hireDate: String?,
        salary: Double?
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Check if username already exists
                val existingUser = userRepository.getUserByUsername(username)
                if (existingUser != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "El nombre de usuario ya existe"
                    )
                    return@launch
                }
                
                val newUser = User(
                    username = username,
                    password = password,
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    role = role,
                    hireDate = hireDate,
                    salary = salary
                )
                
                val userId = userRepository.createUser(newUser)
                if (userId > 0) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showAddUserDialog = false
                    )
                    loadUserStats() // Refresh stats
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al crear el usuario"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun updateUser(
        userId: Long,
        username: String,
        password: String?,
        fullName: String,
        email: String?,
        phone: String?,
        role: UserRole,
        hireDate: String?,
        salary: Double?
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val updatedUser = User(
                    id = userId,
                    username = username,
                    password = password ?: "",
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    role = role,
                    hireDate = hireDate,
                    salary = salary
                )
                
                val success = userRepository.updateUser(updatedUser)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showAddUserDialog = false
                    )
                    loadUserStats() // Refresh stats
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al actualizar el usuario"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun deactivateUser(userId: Long) {
        viewModelScope.launch {
            try {
                val success = userRepository.deactivateUser(userId)
                if (success) {
                    loadUserStats() // Refresh stats
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Error al desactivar el usuario"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}