package com.aditya.expent.data.remote

import android.util.Log
import com.aditya.expent.data.remote.dto.TokenRefreshRequestDto
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

    private companion object {
        const val TAG = "TokenAuthenticator"
    }


    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) return null

        Log.d(TAG, "Received 401 Unauthorized for ${response.request.url.encodedPath}, attempting to refresh token")

        if (response.request.url.encodedPath.contains("auth/refresh")) {

            Log.e(TAG, "Refresh token is invalid or expired")
            return null
        }

        Log.d(TAG, "Attempting to refresh token")
        val user = sessionManager.getUser() ?: return null
        val refreshToken = user.refreshToken

        Log.d(TAG, "Current refresh token: ${refreshToken.take(10)}...${refreshToken.takeLast(10)}")

        synchronized(this) {
            val newUser = sessionManager.getUser()
            val currentToken = newUser?.accessToken

            Log.d(TAG, "Current access token in session: ${currentToken?.take(10)}...${currentToken?.takeLast(10)}")
            
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()

            Log.d(TAG, "Access token used in failed request: ${requestToken?.take(10)}...${requestToken?.takeLast(10)}")

            if (currentToken != null && currentToken != requestToken) {
                Log.d(TAG, "Token already refreshed by another thread, retrying request")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Otherwise, perform the refresh call
            return try {
                Log.d(TAG, "Performing token refresh with refresh token: ${refreshToken.take(10)}...${refreshToken.takeLast(10)}")
                if (refreshToken.isBlank()) {
                    Log.e(TAG, "Refresh token is blank, clearing session")
                    sessionManager.clearSession()
                    return null
                }

                Log.d(TAG, "Attempting to refresh token with: ${refreshToken.take(10)}...${refreshToken.takeLast(10)}")
                
                val refreshResponse = runBlocking {
                    apiServiceProvider.get().refreshToken(TokenRefreshRequestDto(refreshToken))
                }

                Log.d(TAG, "Token refreshed successfully")
                val updatedUser = user.copy(
                    accessToken = refreshResponse.accessToken,
                    refreshToken = refreshResponse.refreshToken
                )

                Log.d("SAVEUSER", "Saving updated user with new tokens: accessToken=${updatedUser.accessToken.take(10)}...${updatedUser.accessToken.takeLast(10)}, refreshToken=${updatedUser.refreshToken.take(10)}...${updatedUser.refreshToken.takeLast(10)}")
                sessionManager.saveUser(updatedUser)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh token: ${e.message}")

                if (e is retrofit2.HttpException && e.code() == 401) {
                    Log.e(TAG, "Refresh token expired/invalid, clearing session")
                    sessionManager.clearSession() // might have problem in going debugs

                }
                
                null
            }
        }
    }
}
