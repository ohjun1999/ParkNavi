package com.jun.parknavi.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jun.parknavi.BuildConfig
import com.jun.parknavi.data.remote.BusStopApi
import com.jun.parknavi.data.remote.ItemsDeserializer
import com.jun.parknavi.data.remote.TagoAuthInterceptor
import com.jun.parknavi.data.remote.dto.ItemsDto
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private const val TAGO_BASE_URL = "https://apis.data.go.kr/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson =
        GsonBuilder()
            .registerTypeAdapter(ItemsDto::class.java, ItemsDeserializer())
            .create()

    @Provides
    @Singleton
    fun provideOkHttpClient(tagoAuthInterceptor: TagoAuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(tagoAuthInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(TAGO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideBusStopApi(retrofit: Retrofit): BusStopApi =
        retrofit.create(BusStopApi::class.java)
}
