package com.arjun.firechat.di

import android.content.Context
import com.arjun.firechat.FireNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext


@Module
@InstallIn(ApplicationComponent::class)
object NotificationManager {

    @Provides
    fun getNotificationManager(@ApplicationContext context: Context): FireNotificationManager {
        return FireNotificationManager(context)
    }

}

