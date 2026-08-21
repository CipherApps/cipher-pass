package dev.cipher.pass.crypto

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PBKDF2_ITERATIONS = 200_000
        private const val PBKDF2_KEY_LENGTH = 256
    }

    fun encryptPassword(plaintext: String, masterPassword: String): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(masterPassword, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = salt + iv + ciphertext
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    fun decryptPassword(cipherB64: String, masterPassword: String): String {
        val combined = android.util.Base64.decode(cipherB64, android.util.Base64.NO_WRAP)
        val salt = combined.sliceArray(0 until SALT_LENGTH)
        val iv = combined.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertext = combined.sliceArray(SALT_LENGTH + IV_LENGTH until combined.size)

        val key = deriveKey(masterPassword, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun hashMasterPassword(password: String): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val combined = salt + key.encoded
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    fun verifyMasterPassword(password: String, hash: String): Boolean {
        return try {
            val combined = android.util.Base64.decode(hash, android.util.Base64.NO_WRAP)
            val salt = combined.sliceArray(0 until SALT_LENGTH)
            val storedKeyBytes = combined.sliceArray(SALT_LENGTH until combined.size)
            val key = deriveKey(password, salt)
            key.encoded.contentEquals(storedKeyBytes)
        } catch (e: Exception) {
            false
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        spec.clearPassword()
        return SecretKeySpec(secretKey.encoded, 0, secretKey.encoded.size, "AES")
    }

    fun passwordStrength(password: String): PasswordStrength {
        var score = 0
        if (password.length >= 8) score += 25
        if (password.length >= 14) score += 20
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score += 20
        if (password.any { it.isDigit() }) score += 15
        if (password.any { !it.isLetterOrDigit() }) score += 20

        return when {
            score < 30 -> PasswordStrength.WEAK
            score < 55 -> PasswordStrength.MODERATE
            score < 80 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }

    fun encryptWithPassword(data: String, password: String): ByteArray {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 100_000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        return salt + iv + encrypted
    }

    fun decryptWithPassword(encryptedData: ByteArray, password: String): String {
        if (encryptedData.size < 28) throw IllegalArgumentException("Invalid backup file format")

        val salt = encryptedData.copyOfRange(0, 16)
        val iv = encryptedData.copyOfRange(16, 28)
        val payload = encryptedData.copyOfRange(28, encryptedData.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 100_000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

        val decrypted = cipher.doFinal(payload)
        return String(decrypted, Charsets.UTF_8)
    }
}

enum class PasswordStrength(val label: String, val fraction: Float) {
    WEAK("Weak", 0.25f),
    MODERATE("Moderate", 0.5f),
    STRONG("Strong", 0.75f),
    VERY_STRONG("Very strong", 1f)
}
