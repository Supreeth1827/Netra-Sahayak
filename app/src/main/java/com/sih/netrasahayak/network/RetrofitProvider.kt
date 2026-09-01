package com.sih.netrasahayak.network

import com.sih.netrasahayak.AppConfig
import com.sih.netrasahayak.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the single Retrofit instance used by the app.
 *
 * The base URL comes from [AppConfig.BASE_URL] (which comes from BuildConfig),
 * so changing servers means editing app/build.gradle.kts - not this file.
 */
object RetrofitProvider {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    // Headers only - retinal images are never dumped to logcat.
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.HEADERS
                        }
                    )
                }
            }
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: NetraApiService by lazy { retrofit.create(NetraApiService::class.java) }

    /**
     * Turns a possibly-relative heatmap path ("/results/abc_heatmap.jpg") into
     * something Coil can load. Absolute URLs and local file paths pass through.
     */
    fun resolveUrl(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        val value = pathOrUrl.trim()
        return when {
            value.startsWith("http://", true) || value.startsWith("https://", true) -> value
            value.startsWith("file://", true) || value.startsWith("/data/") ||
                value.startsWith("/storage/") -> value
            else -> AppConfig.BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
        }
    }
}
