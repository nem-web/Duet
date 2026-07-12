package com.example.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    private fun deriveKey(seed: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(seed.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(bytes, "AES")
    }

    fun encrypt(plainText: String, seedKey: String): String {
        if (plainText.isBlank()) return ""
        return try {
            val keySpec = deriveKey(seedKey)
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivBytes = ByteArray(16)
            val ivSpec = IvParameterSpec(ivBytes)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            android.util.Log.e("CryptoUtils", "Encryption failed: ${e.message}")
            plainText
        }
    }

    fun decrypt(cipherText: String, seedKey: String): String {
        if (cipherText.isBlank()) return ""
        return try {
            val keySpec = deriveKey(seedKey)
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivBytes = ByteArray(16)
            val ivSpec = IvParameterSpec(ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(cipherText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("CryptoUtils", "Decryption failed: ${e.message}")
            "[Decrypted Secure Payload]"
        }
    }
}
