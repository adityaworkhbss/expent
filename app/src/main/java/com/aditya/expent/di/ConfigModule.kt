package com.aditya.expent.di

import android.content.Context
import com.aditya.expent.config.RemoteConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideRemoteConfigManager(@ApplicationContext context: Context): RemoteConfigManager {
        return RemoteConfigManager.getInstance(context)
    }
}