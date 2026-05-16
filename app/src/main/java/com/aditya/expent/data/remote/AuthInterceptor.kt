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
        user?.accessToken?.let { token ->
            val cleanToken = token.trim()
            Log.d("rest re", "Adding Auth Header: Bearer $cleanToken")
            requestBuilder.header("Authorization", "Bearer $cleanToken")
        } ?: Log.d("rest re", "No access token found in session")
        
        return chain.proceed(requestBuilder.build())
    }
}
