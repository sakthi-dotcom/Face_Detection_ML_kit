package com.example.digitest.model

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraRepository(
        @ApplicationContext context: Context,
        @IoExecutor executor: ExecutorService
    ): CameraRepository {
        return CameraRepository(context, executor)
    }

    @Provides
    @Singleton
    @IoExecutor
    fun provideIoExecutor(): ExecutorService {
        return Executors.newSingleThreadExecutor()
    }
}
