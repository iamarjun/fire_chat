package com.arjun.firechat.di

import android.content.Context
import com.arjun.firechat.util.FileUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ApplicationComponent::class)
object IO {
    @Provides
    fun provideFileHelper(@ApplicationContext context: Context): FileUtils =
        FileUtils(context)
}