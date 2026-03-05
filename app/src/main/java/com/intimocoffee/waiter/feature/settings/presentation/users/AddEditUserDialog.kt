package com.intimocoffee.waiter.feature.settings.presentation.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditUserDialog(
    editingUser: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var username by remember { mutableStateOf(editingUser?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf(editingUser?.fullName ?: "") }
    var email by remember { mutableStateOf(editingUser?.email ?: "") }
    var phone by remember { mutableStateOf(editingUser?.phone ?: "") }
    var selectedRole by remember { mutableStateOf(editingUser?.role ?: UserRole.WAITER) }
    var hireDate by remember { mutableStateOf(editingUser?.hireDate ?: "") }
    var salary by remember { mutableStateOf(editingUser?.salary?.toString() ?: "") }
    
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var showRoleDropdown by remember { mutableStateOf(false) }
    
    var usernameError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var fullNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var salaryError by remember { mutableStateOf("") }
    
    val isEditing = editingUser != null
    val title = if (isEditing) "Editar Empleado" else "Nuevo Empleado"
    
    fun validateForm(): Boolean {
        var isValid = true
        
        // Username validation
        if (username.isBlank()) {
            usernameError = "El nombre de usuario es requerido"
            isValid = false
        } else if (username.length < 3) {
            usernameError = "Mínimo 3 caracteres"
            isValid = false
        } else {
            usernameError = ""
        }
        
        // Password validation (required for new users)
        if (!isEditing && password.isBlank()) {
            passwordError = "La contraseña es requerida"
            isValid = false
        } else if (password.isNotBlank() && password.length < 6) {
            passwordError = "Mínimo 6 caracteres"
            isValid = false
        } else if (password.isNotBlank() && password != confirmPassword) {
            passwordError = "Las contraseñas no coinciden"
            isValid = false
        } else {
            passwordError = ""
        }
        
        // Full name validation
        if (fullName.isBlank()) {
            fullNameError = "El nombre completo es requerido"
            isValid = false
        } else {
            fullNameError = ""
        }
        
        // Email validation (optional but if provided must be valid)
        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Email inválido"
            isValid = false
        } else {
            emailError = ""
        }
        
        // Salary validation (optional but if provided must be valid)
        if (salary.isNotBlank()) {
            try {
                val salaryValue = salary.toDouble()
                if (salaryValue < 0) {
                    salaryError = "El salario debe ser positivo"
                    isValid = false
                } else {
                    salaryError = ""
                }
            } catch (e: NumberFormatException) {
                salaryError = "Salario inválido"
                isValid = false
            }
        } else {
            salaryError = ""
        }
        
        return isValid
    }
    
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            usernameError = ""
                        },
                        label = { Text("Nombre de usuario *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isError = usernameError.isNotEmpty(),
                        supportingText = if (usernameError.isNotEmpty()) {
                            { Text(usernameError) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isEditing // Don't allow username changes when editing
                    )
                    
                    // Full Name Field
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { 
                            fullName = it
                            fullNameError = ""
                        },
                        label = { Text("Nombre completo *") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        isError = fullNameError.isNotEmpty(),
                        supportingText = if (fullNameError.isNotEmpty()) {
                            { Text(fullNameError) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Password Field (for new users or password change)
                    if (!isEditing || password.isNotEmpty()) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                passwordError = ""
                            },
                            label = { 
                                Text(if (isEditing) "Nueva contraseña (opcional)" else "Contraseña *") 
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = passwordError.isNotEmpty(),
                            supportingText = if (passwordError.isNotEmpty()) {
                                { Text(passwordError) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Confirm Password Field
                        if (password.isNotEmpty()) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { 
                                    confirmPassword = it
                                    passwordError = ""
                                },
                                label = { Text("Confirmar contraseña *") },
                                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                        Icon(
                                            if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showConfirmPassword) "Ocultar contraseña" else "Mostrar contraseña"
                                        )
                                    }
                                },
                                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else if (isEditing) {
                        // Show button to change password
                        OutlinedButton(
                            onClick = { password = " " }, // Trigger password fields to show
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cambiar contraseña")
                        }
                    }
                    
                    // Email Field (Optional)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = ""
                        },
                        label = { Text("Email (opcional)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = emailError.isNotEmpty(),
                        supportingText = if (emailError.isNotEmpty()) {
                            { Text(emailError) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Phone Field (Optional)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono (opcional)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Role Dropdown
                    ExposedDropdownMenuBox(
                        expanded = showRoleDropdown,
                        onExpandedChange = { showRoleDropdown = !showRoleDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedRole.displayName,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Rol *") },
                            leadingIcon = { Icon(Icons.Default.WorkOutline, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRoleDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showRoleDropdown,
                            onDismissRequest = { showRoleDropdown = false }
                        ) {
                            UserRole.values().forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(role.displayName)
                                            Text(
                                                text = role.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedRole = role
                                        showRoleDropdown = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            when (role) {
                                                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                                UserRole.MANAGER -> Icons.Default.SupervisorAccount
                                                UserRole.WAITER -> Icons.Default.RestaurantMenu
                                                UserRole.BARISTA -> Icons.Default.LocalCafe
                                                UserRole.COOK -> Icons.Default.Restaurant
                                            },
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    // Hire Date Field (Optional)
                    OutlinedTextField(
                        value = hireDate,
                        onValueChange = { hireDate = it },
                        label = { Text("Fecha de contratación (opcional)") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Salary Field (Optional)
                    OutlinedTextField(
                        value = salary,
                        onValueChange = { 
                            salary = it
                            salaryError = ""
                        },
                        label = { Text("Salario (opcional)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = salaryError.isNotEmpty(),
                        supportingText = if (salaryError.isNotEmpty()) {
                            { Text(salaryError) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (validateForm()) {
                                val user = User(
                                    id = editingUser?.id ?: 0L,
                                    username = username,
                                    password = password,
                                    fullName = fullName,
                                    email = email.takeIf { it.isNotBlank() },
                                    phone = phone.takeIf { it.isNotBlank() },
                                    role = selectedRole,
                                    hireDate = hireDate.takeIf { it.isNotBlank() },
                                    salary = salary.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                                )
                                onSave(user)
                            }
                        }
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Save else Icons.Default.PersonAdd,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEditing) "Actualizar" else "Crear")
                    }
                }
            }
        }
    }
}