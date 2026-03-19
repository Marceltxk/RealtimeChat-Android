package com.isacsilva.ufumessenger.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object SupabaseHelper {
    // URL base montada com o ID do seu projeto
    private const val PROJECT_URL = "https://cgqazgjektehsbxsmmns.supabase.co"

    private const val SUPABASE_SECRET_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNncWF6Z2pla3RlaHNieHNtbW5zIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MzY5ODU4MywiZXhwIjoyMDg5Mjc0NTgzfQ.N7-nEH0C6-7pUBFvhowScDhddr-YBncM-2mF_4OC3k0"

    suspend fun uploadFile(context: Context, bucket: String, path: String, fileUri: Uri, mimeType: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$PROJECT_URL/storage/v1/object/$bucket/$path")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $SUPABASE_SECRET_KEY")
                connection.setRequestProperty("Content-Type", mimeType)
                connection.doOutput = true

                context.contentResolver.openInputStream(fileUri)?.use { input ->
                    connection.outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..201) {
                    // Retorna a URL pública bonitinha para a foto aparecer no chat
                    "$PROJECT_URL/storage/v1/object/public/$bucket/$path"
                } else {
                    val error = connection.errorStream?.bufferedReader()?.readText()
                    Log.e("SupabaseHelper", "Erro no upload HTTP $responseCode: $error")
                    null
                }
            } catch (e: Exception) {
                Log.e("SupabaseHelper", "Exceção ao fazer upload", e)
                null
            }
        }
    }

    suspend fun uploadBytes(bucket: String, path: String, bytes: ByteArray, mimeType: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$PROJECT_URL/storage/v1/object/$bucket/$path")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $SUPABASE_SECRET_KEY")
                connection.setRequestProperty("Content-Type", mimeType)
                connection.doOutput = true

                connection.outputStream.use { output ->
                    output.write(bytes)
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..201) {
                    "$PROJECT_URL/storage/v1/object/public/$bucket/$path"
                } else {
                    val error = connection.errorStream?.bufferedReader()?.readText()
                    Log.e("SupabaseHelper", "Erro no upload Bytes HTTP $responseCode: $error")
                    null
                }
            } catch (e: Exception) {
                Log.e("SupabaseHelper", "Exceção ao fazer upload de bytes", e)
                null
            }
        }
    }
}