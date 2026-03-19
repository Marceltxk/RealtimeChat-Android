package com.isacsilva.ufumessenger.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage // Importar FirebaseStorage
import com.isacsilva.ufumessenger.data.repository.UserRepositoryImpl
import com.isacsilva.ufumessenger.domain.contracts.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth,
        storage: FirebaseStorage
    ): UserRepository {
        return UserRepositoryImpl(firestore, firebaseAuth, storage)
    }
}
