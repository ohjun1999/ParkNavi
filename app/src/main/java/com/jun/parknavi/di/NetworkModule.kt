package com.jun.parknavi.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jun.parknavi.BuildConfig
import com.jun.parknavi.data.remote.SeoulAuthInterceptor
import com.jun.parknavi.data.remote.SeoulBusStopApi
import com.jun.parknavi.data.remote.SeoulItemListDeserializer
import com.jun.parknavi.data.remote.dto.SeoulItemListDto
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

// ws.bus.go.kr는 HTTPS를 지원하지 않아 http:// 그대로 쓴다 — 대신 이 도메인만
// cleartext를 허용하도록 res/xml/network_security_config.xml에서 범위를 좁혀뒀다.
private const val SEOUL_BUS_BASE_URL = "http://ws.bus.go.kr/api/rest/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson =
        GsonBuilder()
            .registerTypeAdapter(SeoulItemListDto::class.java, SeoulItemListDeserializer())
            .create()

    @Provides
    @Singleton
    fun provideOkHttpClient(seoulAuthInterceptor: SeoulAuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(seoulAuthInterceptor)
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
            .baseUrl(SEOUL_BUS_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideSeoulBusStopApi(retrofit: Retrofit): SeoulBusStopApi =
        retrofit.create(SeoulBusStopApi::class.java)
}
