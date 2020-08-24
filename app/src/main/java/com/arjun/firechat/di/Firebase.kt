package com.arjun.firechat.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent

@Module
@InstallIn(ApplicationComponent::class)
object Firebase {

    @Provides
    fun getFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    fun getFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    fun getFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}