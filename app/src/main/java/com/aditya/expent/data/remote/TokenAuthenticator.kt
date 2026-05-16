package com.aditya.expent.data.remote

import android.util.Log
import com.aditya.expent.utils.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiServiceProvider: Provider<ApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) return null

        if (response.request.url.encodedPath.contains("auth/refresh")) {

            Log.e("TokenAuthenticator", "Refresh token is invalid or expired")
            return null
        }

        val user = sessionManager.getUser() ?: return null
        val refreshToken = user.refreshToken

        synchronized(this) {
            val newUser = sessionManager.getUser()
            val currentToken = newUser?.accessToken
            
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
            
            if (currentToken != null && currentToken != requestToken) {
                Log.d("TokenAuthenticator", "Token already refreshed by another thread, retrying request")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Otherwise, perform the refresh call
            return try {
                if (refreshToken.isBlank()) {
                    Log.e("TokenAuthenticator", "Refresh token is blank, clearing session")
                    sessionManager.clearSession()
                    return null
                }

                Log.d("TokenAuthenticator", "Attempting to refresh token with: ${refreshToken.take(10)}...${refreshToken.takeLast(10)}")
                
                val refreshResponse = runBlocking {
                    apiServiceProvider.get().refreshToken(com.aditya.expent.data.remote.dto.TokenRefreshRequestDto(refreshToken))
                }

                Log.d("TokenAuthenticator", "Token refreshed successfully")
                val updatedUser = user.copy(
                    accessToken = refreshResponse.accessToken,
                    refreshToken = refreshResponse.refreshToken
                )
                sessionManager.saveUser(updatedUser)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                    .build()
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Failed to refresh token: ${e.message}")

                if (e is retrofit2.HttpException && e.code() == 401) {
                    Log.e("TokenAuthenticator", "Refresh token expired/invalid, clearing session")
                    sessionManager.clearSession()
                }
                
                null
            }
        }
    }
}
