package com.aditya.expent.data.remote

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
        // Only try to refresh if the error is 401
        if (response.code != 401) return null

        val user = sessionManager.getUser() ?: return null
        val refreshToken = user.refreshToken

        synchronized(this) {
            // Re-check user to see if another thread already refreshed the token
            val newUser = sessionManager.getUser()
            val currentToken = newUser?.accessToken
            
            // If the token in the request is already different from the one in session,
            // it means it was already refreshed. Just retry with the new token.
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
            
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Otherwise, perform the refresh call
            return try {
                val refreshResponse = runBlocking {
                    apiServiceProvider.get().refreshToken("refreshToken=$refreshToken")
                }

                val updatedUser = user.copy(
                    accessToken = refreshResponse.accessToken,
                    refreshToken = refreshResponse.refreshToken
                )
                sessionManager.saveUser(updatedUser)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                    .build()
            } catch (e: Exception) {
                // If refresh fails, logout the user or just return null to stop retrying
                android.util.Log.e("TokenAuthenticator", "Failed to refresh token", e)
                null
            }
        }
    }
}
