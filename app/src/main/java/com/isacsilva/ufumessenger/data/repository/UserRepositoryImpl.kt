package com.isacsilva.ufumessenger.data.repository

import android.net.Uri // Importar Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage // IMPORTAR FirebaseStorage
import com.isacsilva.ufumessenger.domain.contracts.UserRepository
import com.isacsilva.ufumessenger.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val storage: FirebaseStorage
) : UserRepository {

    override suspend fun saveFcmToken(token: String): Result<Unit> {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            return Result.failure(Exception("Utilizador não autenticado para guardar o token FCM."))
        }
        return try {
            firestore.collection("users").document(currentUserId)
                .update("fcmToken", token)
                .await()
            Log.d("UserRepository", "Token FCM guardado com sucesso no Firestore.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Erro ao guardar o token FCM", e)
            Result.failure(e)
        }
    }

    override suspend fun searchUsersByUsername(query: String): Result<List<User>> {
        return try {
            if (query.isBlank()) {
                return Result.success(emptyList())
            }

            val lowercaseQuery = query.lowercase()


            Log.d("UserSearchRepo", "Buscando por (case-insensitive): '$lowercaseQuery'")


            val result = firestore.collection("users")
                .whereGreaterThanOrEqualTo("username_lowercase", lowercaseQuery)
                .whereLessThanOrEqualTo("username_lowercase", lowercaseQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()

            Log.d("UserSearchRepo", "Firestore encontrou ${result.size()} documentos.")

            val currentUserId = firebaseAuth.currentUser?.uid
            val users = result.toObjects(User::class.java).filter { user ->
                currentUserId == null || user.uid != currentUserId
            }


            Log.d("UserSearchRepo", "Após filtrar o usuário atual (se houver), restaram ${users.size} usuários.")

            Result.success(users)
        } catch (e: Exception) {
            Log.e("UserSearchRepo", "Erro na busca: ", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("UserSearchRepo", "Error getting user by ID", e)
            Result.failure(e)
        }
    }


    override suspend fun updateUserProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("profile_pictures/$userId/profile.jpg")

            val uploadTask = storageRef.putFile(imageUri).await()

            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            firestore.collection("users").document(userId)
                .update("profilePictureUrl", downloadUrl)
                .await()

            Log.d("UserRepositoryImpl", "Profile picture updated successfully. URL: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error updating profile picture", e)
            Result.failure(e)
        }
    }

    override fun getContacts(): Flow<List<User>> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return flowOf(emptyList())


        return firestore.collection("users").document(currentUserId)
            .snapshots()
            .map { snapshot ->
                snapshot.get("contacts") as? List<String> ?: emptyList()
            }
            .flatMapLatest { contactIds ->
                if (contactIds.isNotEmpty()) {
                    firestore.collection("users")
                        .whereIn(FieldPath.documentId(), contactIds)
                        .snapshots()
                        .map { querySnapshot -> querySnapshot.toObjects(User::class.java) }
                } else {
                    flowOf(emptyList())
                }
            }
    }

    override fun getUsersByIds(userIds: List<String>): Flow<Result<List<User>>> = callbackFlow {
        if (userIds.isEmpty()) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .whereIn(FieldPath.documentId(), userIds)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                    trySend(Result.success(users))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addContact(contactId: String): Result<Unit> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Utilizador não autenticado."))
        return try {
            firestore.collection("users").document(currentUserId)
                .update("contacts", FieldValue.arrayUnion(contactId)) // Adiciona o ID ao array
                .await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun removeContact(contactId: String): Result<Unit> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Utilizador não autenticado."))
        return try {
            firestore.collection("users").document(currentUserId)
                .update("contacts", FieldValue.arrayRemove(contactId)) // Remove o ID do array
                .await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateUserProfile(
        userId: String,
        newUsername: String?,
        newEmail: String?,
        newBirthDate: String?,
        newStatus: String? // Este parâmetro agora representa o userSetStatus
    ): Result<Unit> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be blank."))
        }

        return try {
            val updatesMap = mutableMapOf<String, Any?>()

            newUsername?.let {
                if (it.isNotBlank()) {
                    updatesMap["username"] = it
                    updatesMap["username_lowercase"] = it.lowercase()
                }
            }

            newEmail?.let {
                updatesMap["email"] = it
            }

            newBirthDate?.let {
                updatesMap["birthDate"] = it
            }

            // ATUALIZADO: Salvar o newStatus no campo "userSetStatus"
            newStatus?.let {
                // Se o status for uma string vazia, você pode querer removê-lo
                // ou definir um valor padrão. O dropdown não deve enviar string vazia
                // se "Disponível" for o default para UI quando o valor real for vazio/nulo.
                // Se você quiser explicitamente salvar uma string vazia como userSetStatus:
                // updatesMap["userSetStatus"] = it
                // Se você quiser que uma string vazia no dropdown signifique "sem userSetStatus" (nulo):
                if (it.isNotBlank()) { // Salva apenas se não for branco.
                    updatesMap["userSetStatus"] = it
                } else {
                    // Se o status editado for explicitamente tornado branco
                    // (ex: por uma opção "Limpar Status" no dropdown, que não temos agora),
                    // você pode querer deletar o campo ou setá-lo para null.
                    // Por enquanto, se vier branco, não faz nada,
                    // mas o ProfileViewModel deve enviar o valor selecionado ("Disponível", "Ocupado").
                    // Se o dropdown envia "Disponível", será salvo "Disponível".
                    // Se o userSetStatus original era "Ocupado" e o usuário seleciona "Disponível",
                    // "Disponível" será salvo.
                    updatesMap["userSetStatus"] = it // Salva o status como está (ex: "Disponível", "Ocupado")
                    // Se o usuário pudesse limpar para uma string vazia,
                    // e você quisesse tratar isso como "sem status definido",
                    // poderia ser: FieldValue.delete() ou null.
                    // Mas o dropdown atual não permite isso.
                }
            }

            if (updatesMap.isEmpty()) {
                // Se nenhum campo foi modificado (incluindo o userSetStatus), não faz nada.
                return Result.success(Unit)
            }

            firestore.collection("users").document(userId)
                .update(updatesMap)
                .await()

            Log.d("UserRepositoryImpl", "User profile updated successfully for userId: $userId. Updates: $updatesMap")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error updating user profile for userId: $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid

            val snapshot = firestore.collection("users")
                .get()
                .await()

            val users = snapshot.toObjects(User::class.java)
                .filter { user -> user.uid != currentUserId }

            Log.d("UserRepositoryImpl", "getAllUsers: ${users.size} usuários encontrados.")
            Result.success(users)
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Erro ao buscar todos os usuários", e)
            Result.failure(e)
        }
    }
}
