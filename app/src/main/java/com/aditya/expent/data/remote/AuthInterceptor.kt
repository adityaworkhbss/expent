package com.aditya.expent.data.remote

import com.aditya.expent.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        
        val user = sessionManager.getUser()
        user?.accessToken?.let { token ->
            val cleanToken = token.trim()
            android.util.Log.d("rest re", "Adding Auth Header: Bearer $cleanToken")
            requestBuilder.addHeader("Authorization", "Bearer $cleanToken")
        } ?: android.util.Log.d("rest re", "No access token found in session")
        
        return chain.proceed(requestBuilder.build())
    }
}
