package com.scenescribe.app.data.api

import android.content.Context
import com.scenescribe.app.BuildConfig
import com.scenescribe.app.data.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val PUBLIC_PATHS = setOf("auth/login", "auth/register", "auth/verify")

    fun create(context: Context): ApiService {
        val tokenManager = TokenManager(context)

        val authInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath.trimStart('/')
            val isPublic = PUBLIC_PATHS.any { path.contains(it) }

            val newRequest = if (!isPublic) {
                val token = tokenManager.getToken()
                if (token != null) {
                    request.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else request
            } else request

            val response = chain.proceed(newRequest)

            if (response.code == 401 && !isPublic) {
                tokenManager.clear()
            }

            response
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
