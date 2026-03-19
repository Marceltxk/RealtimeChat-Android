package com.isacsilva.ufumessenger.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage // IMPORTAR FirebaseStorage
import com.isacsilva.ufumessenger.data.repository.authentication.FirebaseAuthRepositoryImpl
import com.isacsilva.ufumessenger.domain.contracts.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance() // ADICIONAR ESTA FUNÇÃO

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore

    ): AuthRepository {
        return FirebaseAuthRepositoryImpl(firebaseAuth, firestore)
    }
}
