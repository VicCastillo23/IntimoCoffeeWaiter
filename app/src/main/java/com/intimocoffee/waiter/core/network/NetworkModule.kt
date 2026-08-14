package com.intimocoffee.waiter.core.network

import com.intimocoffee.waiter.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideIntimoCoffeeApiService(
        dynamicRetrofitProvider: DynamicRetrofitProvider
    ): IntimoCoffeeApiService {
        // Prefer live DynamicRetrofitProvider; on physical devices before discovery
        // getApiService() throws — callers should use DynamicRetrofitProvider after discover.
        return try {
            dynamicRetrofitProvider.getApiService()
        } catch (_: IllegalStateException) {
            // Hilt may resolve this before NSD finishes; placeholder is unused by RemoteOrderService
            // which always calls DynamicRetrofitProvider.getApiService().
            Retrofit.Builder()
                .baseUrl("http://127.0.0.1:9/")
                .addConverterFactory(
                    Json { ignoreUnknownKeys = true }
                        .asConverterFactory("application/json".toMediaType())
                )
                .build()
                .create(IntimoCoffeeApiService::class.java)
        }
    }

    @Provides
    @Singleton
    @Named("aws")
    fun provideAwsRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(loyaltyRetrofitBaseUrl())
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAwsLoyaltyApiService(
        @Named("aws") retrofit: Retrofit
    ): AwsLoyaltyApiService = retrofit.create(AwsLoyaltyApiService::class.java)

    private fun loyaltyRetrofitBaseUrl(): String {
        val u = BuildConfig.LOYALTY_API_BASE_URL.trim()
        return if (u.endsWith("/")) u else "$u/"
    }
}
