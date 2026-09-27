package com.aditya.expent.data.remote

import com.aditya.expent.utils.AppLogger
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLoggingInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 1. Capture request payload
        var requestPayload: String? = null
        val requestBody = request.body
        if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            val charset = requestBody.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
            requestPayload = buffer.readString(charset)
        }

        // 2. Extract request headers (masked)
        val headersMap = mutableMapOf<String, String>()
        for (i in 0 until request.headers.size) {
            headersMap[request.headers.name(i)] = request.headers.value(i)
        }

        AppLogger.apiRequest(
            method = request.method,
            url = request.url.toString(),
            headers = headersMap,
            payload = requestPayload
        )

        // 3. Execute request and measure duration
        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            AppLogger.apiError(
                method = request.method,
                url = request.url.toString(),
                error = e.message,
                durationMs = tookMs,
                throwable = e
            )
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        // 4. Capture response payload without consuming the body stream
        val responseBody = response.body
        var responsePayload: String? = null
        if (responseBody != null) {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            val charset = responseBody.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
            responsePayload = buffer.clone().readString(charset)
        }

        AppLogger.apiResponse(
            method = request.method,
            url = request.url.toString(),
            code = response.code,
            durationMs = tookMs,
            payload = responsePayload
        )

        return response
    }
}
