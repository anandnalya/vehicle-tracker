package com.anandnalya.vehicletracker.di

import com.anandnalya.vehicletracker.data.network.LenientJsonConverterFactory
import com.anandnalya.vehicletracker.data.network.SessionCookieInterceptor
import com.anandnalya.vehicletracker.data.network.TrackingApiService
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        sessionInterceptor: SessionCookieInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(sessionInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson() = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TrackingApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(LenientJsonConverterFactory())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()
    }

    @Provides
    @Singleton
    fun provideTrackingApiService(retrofit: Retrofit): TrackingApiService {
        return retrofit.create(TrackingApiService::class.java)
    }
}
