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
            android.util.Log.d("rest re", "Adding Auth Header: Bearer $token")
            requestBuilder.addHeader("Authorization", "Bearer $token")
        } ?: android.util.Log.d("rest re", "No access token found in session")
        
        return chain.proceed(requestBuilder.build())
    }
}
