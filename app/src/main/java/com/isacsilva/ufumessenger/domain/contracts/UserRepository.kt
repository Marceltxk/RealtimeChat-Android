package com.isacsilva.ufumessenger.domain.contracts

import android.net.Uri
import com.isacsilva.ufumessenger.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun searchUsersByUsername(query: String): Result<List<User>>
    suspend fun getUserById(userId: String): Result<User?>
    suspend fun updateUserProfilePicture(userId: String, imageUri: Uri): Result<String>

    suspend fun saveFcmToken(token: String): Result<Unit>
    fun getContacts(): Flow<List<User>>
    suspend fun addContact(contactId: String): Result<Unit>
    suspend fun removeContact(contactId: String): Result<Unit>

    fun getUsersByIds(userIds: List<String>): Flow<Result<List<User>>>

    suspend fun updateUserProfile(userId: String, newUsername: String?, newEmail: String?, newBirthDate: String?, newStatus: String?): Result<Unit>

    suspend fun getAllUsers(): Result<List<User>>
}