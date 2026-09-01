package com.chirihome.platform.domain.auth

import com.chirihome.platform.repository.auth.AuthRepository
import retrofit2.HttpException
import java.io.IOException

class ValidateSessionUseCase(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(): SessionValidationResult {
        return try {
            authRepository.getCurrentUser()

            SessionValidationResult.Authenticated
        } catch (exception: HttpException) {
            if (exception.code() == 401) {
                SessionValidationResult.Unauthenticated
            } else {
                SessionValidationResult.Error(exception)
            }
        } catch (exception: IOException) {
            SessionValidationResult.Error(exception)
        } catch (exception: Exception) {
            SessionValidationResult.Error(exception)
        }
    }
}