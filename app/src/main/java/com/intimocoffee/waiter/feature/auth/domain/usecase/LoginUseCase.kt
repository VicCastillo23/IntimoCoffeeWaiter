package com.intimocoffee.waiter.feature.auth.domain.usecase

import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<User> {
        return try {
            authRepository.login(username, password).fold(
                onSuccess = { user ->
                    authRepository.saveCurrentUser(user)
                    Result.success(user)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}