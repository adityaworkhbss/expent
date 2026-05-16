package com.aditya.expent.data.remote

import android.util.Log
import com.aditya.expent.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        val path = request.url.encodedPath
        if (path.contains("auth/google") || path.contains("auth/refresh")) {
            return chain.proceed(request)
        }

        val user = sessionManager.getUser()
        val token = user?.accessToken
        
        if (token != null) {
            val cleanToken = token.trim()
            if (cleanToken.isNotEmpty()) {
                Log.d("AuthInterceptor", "Adding Auth Header for $path: Bearer ${cleanToken.take(10)}...")
                requestBuilder.header("Authorization", "Bearer $cleanToken")
            } else {
                Log.w("AuthInterceptor", "Access token is empty for $path")
            }
        } else {
            Log.w("AuthInterceptor", "No user or access token found in session for $path")
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
