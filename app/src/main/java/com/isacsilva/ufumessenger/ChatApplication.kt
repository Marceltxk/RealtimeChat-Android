package com.isacsilva.ufumessenger

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChatApplication : Application() {

    companion object {
        private const val TAG = "ChatApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ChatApplication - BuildConfig.DEBUG = ${BuildConfig.DEBUG}")

        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "FirebaseApp.initializeApp(this) SUCESSO")

            initializeFirestore()

            initializeAppCheck()

        } catch (e: Exception) {
            Log.e(TAG, "Erro GERAL na inicialização do Firebase em onCreate: ${e.message}", e)
        }
        Log.d(TAG, "onCreate: ChatApplication - FIM da inicialização (com Firestore e AppCheck)")
    }

    private fun initializeFirestore() {
        try {
            val firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings
            Log.d(TAG, "Firestore configurado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar Firestore: ${e.message}", e)
        }
    }


    private fun initializeAppCheck() {
        try {
            Log.d(TAG, "Configurando App Check...")
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            Log.d(TAG, "FirebaseAppCheck.getInstance() SUCESSO. Instância: $firebaseAppCheck")

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "MODO DEBUG DETECTADO (BuildConfig.DEBUG == true)")
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "App Check configurado em modo DEBUG")
            } else {
                Log.d(TAG, "MODO PRODUÇÃO DETECTADO (BuildConfig.DEBUG == false)")
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "App Check configurado em modo PRODUÇÃO")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro CRÍTICO ao configurar o App Check: ${e.message}", e)
        }
    }
}

