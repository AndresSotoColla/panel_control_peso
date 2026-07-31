package com.example.panel_control_peso.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Default API Server URL (Blueprint consultor)
    var BASE_URL = "https://interno.control.agricolaguapa.com/consultor/"
        private set

    fun setCustomBaseUrl(url: String) {
        var formattedUrl = url.trim()
        if (!formattedUrl.endsWith("/")) {
            formattedUrl += "/"
        }
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        BASE_URL = formattedUrl
        apiServiceInstance = null
    }

    private var apiServiceInstance: ApiService? = null

    val apiService: ApiService
        get() {
            if (apiServiceInstance == null) {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                apiServiceInstance = retrofit.create(ApiService::class.java)
            }
            return apiServiceInstance!!
        }
}
