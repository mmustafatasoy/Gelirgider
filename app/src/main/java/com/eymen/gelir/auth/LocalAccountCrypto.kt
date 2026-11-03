package com.eymen.gelir.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Pure Kotlin/JVM password functions. No password/recovery code is stored in plaintext.
 * Offline login is a local app lock, not encryption of the Room database or ZIP archives.
 */
object LocalAccountCrypto {
    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private val random = SecureRandom()
    private val hexChars = "0123456789ABCDEF"

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

    fun toHex(value: ByteArray): String = buildString(value.size * 2) {
        value.forEach { b ->
            val byte = b.toInt() and 0xff
            append(hexChars[byte ushr 4])
            append(hexChars[byte and 0x0f])
        }
    }

    fun fromHex(value: String): ByteArray {
        require(value.length % 2 == 0 && value.all { it in "0123456789ABCDEFabcdef" })
        return ByteArray(value.length / 2) { i ->
            value.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun passwordHash(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun equalsConstantTime(a: ByteArray, b: ByteArray): Boolean = MessageDigest.isEqual(a, b)

    /** Strip list numbering (e.g. "1. CODE") and keyboard separators before validation. */
    fun canonicalRecoveryCode(code: String): String =
        code.trim()
            .replace(Regex("""^[1-8][.)]\s*"""), "")
            .uppercase(java.util.Locale.ROOT)
            .replace(Regex("""[\s\p{Zs}\p{Pd}]"""), "")

    /** 96 bits entropy; six groups of four hex digits for printable offline recovery. */
    fun newRecoveryCode(): String =
        toHex(randomBytes(12)).chunked(4).joinToString("-")

    /** 128 bits entropy, displayed exactly once as a temporary password. */
    fun newTemporaryPassword(): String =
        toHex(randomBytes(16)).chunked(4).joinToString("-")

    fun recoveryDigest(code: String, salt: ByteArray): String {
        val normalized = canonicalRecoveryCode(code)
        require(normalized.length == 24 && normalized.all { it in '0'..'9' || it in 'A'..'F' })
        val bytes = salt + normalized.toByteArray(Charsets.US_ASCII)
        return toHex(MessageDigest.getInstance("SHA-256").digest(bytes))
    }
}
