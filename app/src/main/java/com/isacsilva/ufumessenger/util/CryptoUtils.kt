package com.isacsilva.ufumessenger.util

import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    // Chave de 16 bytes exatos (128 bits) - O coração da nossa segurança
    private const val KEY = "UfuMessenger2026"
    private const val ALGORITHM = "AES"

    fun encrypt(data: String): String {
        return try {
            val secretKey = SecretKeySpec(KEY.toByteArray(), ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(data.toByteArray())
            // Converte para Base64 para salvar como texto normal no Firestore
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("CryptoUtils", "Erro ao criptografar", e)
            data // Fallback: envia sem criptografia se der falha grave
        }
    }

    fun decrypt(data: String): String {
        return try {
            val secretKey = SecretKeySpec(KEY.toByteArray(), ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(data, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            // Se cair aqui, é porque a mensagem é antiga e não estava criptografada.
            // Retornamos o texto original para não quebrar o chat!
            data
        }
    }
}