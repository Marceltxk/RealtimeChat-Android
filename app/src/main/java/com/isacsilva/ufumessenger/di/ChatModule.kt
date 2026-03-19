package com.isacsilva.ufumessenger.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.isacsilva.ufumessenger.data.repository.ChatRepositoryImpl
import com.isacsilva.ufumessenger.domain.contracts.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideChatRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth,
        storage: FirebaseStorage
    ): ChatRepository {

        return ChatRepositoryImpl(context, firestore, firebaseAuth, storage)
    }
}