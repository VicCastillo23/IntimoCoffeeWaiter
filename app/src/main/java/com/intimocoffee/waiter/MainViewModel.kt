package com.intimocoffee.waiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    /**
     * True if a local session exists and POS revalidation succeeds.
     * Clears DataStore when revalidation fails.
     */
    suspend fun isUserLoggedIn(): Boolean {
        if (!authRepository.isLoggedIn()) return false
        return authRepository.revalidateSession()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
