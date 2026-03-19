package com.isacsilva.ufumessenger.data.repository.authentication

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.isacsilva.ufumessenger.domain.contracts.AuthRepository
import com.isacsilva.ufumessenger.domain.contracts.AuthState
import com.isacsilva.ufumessenger.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FirebaseAuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val _authState = MutableStateFlow(AuthState(isInitialLoading = true))
    private var userDocumentListener: ListenerRegistration? = null

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                userDocumentListener?.remove()
                userDocumentListener = firestore.collection("users").document(firebaseUser.uid)
                    .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                        if (firebaseFirestoreException != null) {
                            Log.w(
                                "FirebaseAuthRepo",
                                "Listen error no documento do usuário",
                                firebaseFirestoreException
                            )
                            _authState.update {
                                it.copy(
                                    user = firebaseUser.toDomainUser(
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                    ), // Adicionado campos faltantes
                                    isInitialLoading = false
                                )
                            }
                            return@addSnapshotListener
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            val userFromFirestore = documentSnapshot.toObject(User::class.java)
                            _authState.update {
                                it.copy(
                                    user = firebaseUser.toDomainUser(
                                        usernameFromFirestore = userFromFirestore?.username,
                                        usernameLowercaseFromFirestore = userFromFirestore?.username_lowercase,
                                        profilePictureUrlFromFirestore = userFromFirestore?.profilePictureUrl,
                                        emailFromFirestore = userFromFirestore?.email,
                                        birthDateFromFirestore = userFromFirestore?.birthDate,
                                        presenceStatusFromFirestore = userFromFirestore?.presenceStatus, // Novo
                                        lastSeenFromFirestore = userFromFirestore?.lastSeen // Novo
                                    ),
                                    isInitialLoading = false
                                )
                            }
                        } else {
                            Log.w(
                                "FirebaseAuthRepo",
                                "Documento do usuário não encontrado ou nulo no snapshot listener."
                            )
                            _authState.update {
                                it.copy(
                                    user = firebaseUser.toDomainUser(
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                    ), // Adicionado campos faltantes
                                    isInitialLoading = false
                                )
                            }
                        }
                    }
            } else {
                userDocumentListener?.remove()
                userDocumentListener = null
                _authState.update { it.copy(user = null, isInitialLoading = false) }
            }
        }
    }

    override suspend fun checkIfPhoneNumberExists(phoneNumber: String): Result<Boolean> {
        return try {
            val query = firestore.collection("users")
                .whereEqualTo("phone", phoneNumber)
                .limit(1)
                .get()
                .await()

            Result.success(!query.isEmpty)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepo", "Erro ao verificar se o número de telefone existe", e)
            Result.failure(e)
        }
    }

    override fun getAuthState(): StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun verifyPhoneNumber(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override suspend fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential,
        username: String
    ): Result<Unit> {
        return try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false

            if (firebaseUser != null) {
                val usernameLowercase = username.lowercase()

                if (isNewUser) {
                    val userDocument = mutableMapOf<String, Any?>(
                        "uid" to firebaseUser.uid,
                        "username" to username,
                        "username_lowercase" to usernameLowercase,
                        "phone" to firebaseUser.phoneNumber,
                        "profilePictureUrl" to null,
                        "email" to null,
                        "birthDate" to null,
                        "presenceStatus" to "Online", // Valor inicial ao criar novo usuário
                        "lastSeen" to null // Valor inicial ao criar novo usuário
                    )

                    val usernameQuery = firestore.collection("users")
                        .whereEqualTo("username_lowercase", usernameLowercase)
                        .get()
                        .await()

                    if (!usernameQuery.isEmpty) {
                        firebaseUser.delete().await()
                        return Result.failure(Exception("Username already taken. Please choose another one."))
                    }

                    val userRef = firestore.collection("users").document(firebaseUser.uid)
                    userRef.set(userDocument).await()
                } else {
                    // Para usuários existentes, apenas atualiza o status para Online
                    // lastSeen não é alterado aqui, pois ele só é atualizado ao ficar offline.
                    firestore.collection("users").document(firebaseUser.uid)
                        .update("presenceStatus", "Online")
                        .await()
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sign in (Firebase Auth user is null)."))
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.w("FirebaseAuthRepo", "Sign in failed: Invalid verification code.", e)
            Result.failure(Exception("The verification code is invalid."))
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepo", "Sign in failed: ${e.message}", e)
            Result.failure(Exception("An unexpected error occurred during sign in: ${e.message}"))
        }
    }

    override fun signOut() {
        // Atualizar status para Offline antes de fazer signOut
        // Não podemos usar _authState.value.user?.uid aqui diretamente porque o listener pode já ter limpado o usuário
        // É mais seguro pegar o UID direto do firebaseAuth.currentUser antes do signOut
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId != null) {
            val updates = mapOf(
                "presenceStatus" to "Offline",
                "lastSeen" to System.currentTimeMillis()
            )
            firestore.collection("users").document(currentUserId)
                .update(updates)
                .addOnFailureListener { e ->
                    Log.w(
                        "FirebaseAuthRepo",
                        "Falha ao atualizar status para offline no signOut",
                        e
                    )
                }
        }
        firebaseAuth.signOut()
    }

    // Implementação do novo método
    override suspend fun updateUserStatus(userId: String, presenceStatus: String, lastSeen: Long?) {
        val userRef = firestore.collection("users").document(userId)
        val updates = mutableMapOf<String, Any?>()
        updates["presenceStatus"] = presenceStatus
        if (presenceStatus == "Offline") {
            updates["lastSeen"] = lastSeen
        } else if (presenceStatus == "Online") {
            if (lastSeen != null) {
                updates["lastSeen"] = lastSeen
            }
        }
        Log.d("FirebaseAuthRepo", "updateUserStatus: Preparando para atualizar Firestore para userId=$userId com dados: $updates")
        try {
            if (presenceStatus == "Offline") {
                // Para Offline, vamos tentar "fire-and-forget" para diagnóstico
                // Removendo o await TEMPORARIAMENTE para este caso.
                userRef.update(updates)
                    .addOnSuccessListener {
                        Log.d("FirebaseAuthRepo", "Status do usuário $userId ATUALIZADO (sem await) NO FIRESTORE para $presenceStatus, lastSeen: $lastSeen")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseAuthRepo", "Erro ao ATUALIZAR status (sem await) do usuário $userId: ${e.javaClass.simpleName} - ${e.message}", e)
                    }
            } else {
                // Para Online, mantemos o await, pois geralmente ocorre quando o app está ativo
                userRef.update(updates).await()
                Log.d("FirebaseAuthRepo", "Status do usuário $userId ATUALIZADO (com await) NO FIRESTORE para $presenceStatus, lastSeen: $lastSeen")
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepo", "Exceção no updateUserStatus (possivelmente do await para Online) para userId $userId: ${e.javaClass.simpleName} - ${e.message}", e)
        }
    }
    // Adapte a função toDomainUser para incluir os novos campos
    private fun FirebaseUser.toDomainUser(
        usernameFromFirestore: String?,
        usernameLowercaseFromFirestore: String?,
        profilePictureUrlFromFirestore: String?,
        emailFromFirestore: String?,
        birthDateFromFirestore: String?,
        presenceStatusFromFirestore: String?, // Novo
        lastSeenFromFirestore: Long? // Novo
    ): com.isacsilva.ufumessenger.domain.model.User {
        return com.isacsilva.ufumessenger.domain.model.User(
            uid = this.uid,
            username = usernameFromFirestore,
            username_lowercase = usernameLowercaseFromFirestore,
            profilePictureUrl = profilePictureUrlFromFirestore,
            phone = this.phoneNumber,
            email = emailFromFirestore,
            birthDate = birthDateFromFirestore,
            fcmToken = null, // Mantenha como está se não estiver usando ainda
            presenceStatus = presenceStatusFromFirestore ?: "Offline", // Valor padrão
            lastSeen = lastSeenFromFirestore // Pode ser null
        )
    }
}
