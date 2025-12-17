package com.anandnalya.vehicletracker.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that adds session cookies to requests
 * and saves new session cookies from responses
 */
@Singleton
class SessionCookieInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Add cookies and required headers
        val cookies = runBlocking { sessionManager.getCookiesSync() }
        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "*/*")
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("Origin", "https://tracknovate.in")
            .header("Referer", "https://tracknovate.in/jsp/quickview.jsp")
            .header("X-Requested-With", "XMLHttpRequest")

        if (cookies.isNotEmpty()) {
            requestBuilder.header("Cookie", cookies)
        }

        val response = chain.proceed(requestBuilder.build())

        // Save all cookies from response
        val newCookies = mutableListOf<String>()
        response.headers("Set-Cookie").forEach { cookie ->
            val cookiePart = cookie.split(";").firstOrNull()?.trim()
            if (!cookiePart.isNullOrEmpty()) {
                newCookies.add(cookiePart)
            }
        }
        if (newCookies.isNotEmpty()) {
            runBlocking { sessionManager.saveCookies(newCookies.joinToString("; ")) }
        }

        return response
    }
}
