package com.jun.parknavi.di

import com.jun.parknavi.data.repository.BusStopRepository
import com.jun.parknavi.data.repository.BusStopRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBusStopRepository(impl: BusStopRepositoryImpl): BusStopRepository
}
