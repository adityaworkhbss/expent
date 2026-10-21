package com.aditya.expent.data.remote

import com.aditya.expent.config.AppConfigStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ServerUrlInterceptor @Inject constructor(
    private val appConfigStore: AppConfigStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val server = appConfigStore.awaitServerUrl()
        if (request.url.host == server.host && request.url.scheme == server.scheme && request.url.port == server.port) {
            return chain.proceed(request)
        }
        val url = request.url.newBuilder()
            .scheme(server.scheme)
            .host(server.host)
            .port(server.port)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}
