package com.eymen.gelir.auth

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

/**
 * One local profile per app install. The app does not register with any external server.
 *
 * Important: SharedPreferences holds salted hashes, not passwords, but the SQLite
 * database and exported ZIP archives are NOT encrypted by this local lock.
 */
class LocalAccountStore(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("eymen_local_account_v1", Context.MODE_PRIVATE)

    fun hasAccount(): Boolean = preferences.contains("account_user") &&
        preferences.contains("password_hash") && preferences.contains("password_salt")

    fun username(): String = preferences.getString("account_user", "").orEmpty()
    fun mustChangeTemporaryPassword(): Boolean = preferences.getBoolean("temporary_only", false)

    @Synchronized
    fun register(user: String, password: String): List<String> {
        check(!hasAccount()) { "Bu telefonda zaten kullanıcı kayıtlı." }
        val cleanUser = user.trim()
        require(validUsername(cleanUser)) { "Kullanıcı adı 3–32 karakter olmalı; boşluk içermemeli." }
        require(validPassword(password)) { "Şifre en az 10 karakter olmalı." }
        val salt = LocalAccountCrypto.randomBytes(16)
        val recoverSalt = LocalAccountCrypto.randomBytes(16)
        val codes = List(8) { LocalAccountCrypto.newRecoveryCode() }
        val hashedCodes = codes.map { LocalAccountCrypto.recoveryDigest(it, recoverSalt) }.toSet()
        val hashed = LocalAccountCrypto.passwordHash(password.toCharArray(), salt)
        check(
            preferences.edit()
                .putString("account_user", cleanUser)
                .putString("password_salt", LocalAccountCrypto.toHex(salt))
                .putString("password_hash", LocalAccountCrypto.toHex(hashed))
                .putString("recovery_salt", LocalAccountCrypto.toHex(recoverSalt))
                .putStringSet("recovery_hashes", hashedCodes)
                .putBoolean("temporary_only", false)
                .commit()
        ) { "Kullanıcı kaydı diske yazılamadı." }
        return codes
    }

    @Synchronized
    fun authenticate(user: String, password: String): Boolean {
        if (!hasAccount() || mustChangeTemporaryPassword()) return false
        if (!username().equals(user.trim(), ignoreCase = true)) return false
        return checkSecret(password)
    }

    @Synchronized
    fun redeemRecovery(user: String, code: String): String? {
        if (!hasAccount() || !username().equals(user.trim(), ignoreCase = true)) return null
        val digest = try {
            val salt = LocalAccountCrypto.fromHex(preferences.getString("recovery_salt", "").orEmpty())
            LocalAccountCrypto.recoveryDigest(code, salt)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val old = preferences.getStringSet("recovery_hashes", emptySet()).orEmpty()
        if (!old.contains(digest)) return null
        val temp = LocalAccountCrypto.newTemporaryPassword()
        val salt = LocalAccountCrypto.randomBytes(16)
        val hashed = LocalAccountCrypto.passwordHash(temp.toCharArray(), salt)
        val remaining = old.toMutableSet().apply { remove(digest) }
        val saved = preferences.edit()
            .putStringSet("recovery_hashes", remaining)
            .putString("password_salt", LocalAccountCrypto.toHex(salt))
            .putString("password_hash", LocalAccountCrypto.toHex(hashed))
            .putBoolean("temporary_only", true)
            .commit()
        return if (saved) temp else null
    }

    @Synchronized
    fun replaceTemporaryPassword(tempPassword: String, newPassword: String): Boolean {
        if (!hasAccount() || !mustChangeTemporaryPassword() || !validPassword(newPassword)) return false
        if (!checkSecret(tempPassword)) return false
        val salt = LocalAccountCrypto.randomBytes(16)
        val hashed = LocalAccountCrypto.passwordHash(newPassword.toCharArray(), salt)
        return preferences.edit()
            .putString("password_salt", LocalAccountCrypto.toHex(salt))
            .putString("password_hash", LocalAccountCrypto.toHex(hashed))
            .putBoolean("temporary_only", false)
            .commit()
    }

    /** Requires the active password; generated codes are visible only once. */
    @Synchronized
    fun regenerateRecoveryCodes(userPassword: String): List<String>? {
        if (!hasAccount() || mustChangeTemporaryPassword() || !checkSecret(userPassword)) return null
        val salt = LocalAccountCrypto.randomBytes(16)
        val codes = List(8) { LocalAccountCrypto.newRecoveryCode() }
        val hashes = codes.map { LocalAccountCrypto.recoveryDigest(it, salt) }.toSet()
        if (!preferences.edit()
                .putString("recovery_salt", LocalAccountCrypto.toHex(salt))
                .putStringSet("recovery_hashes", hashes)
                .commit()
        ) return null
        return codes
    }

    private fun checkSecret(candidate: String): Boolean = try {
        val salt = LocalAccountCrypto.fromHex(preferences.getString("password_salt", "").orEmpty())
        val stored = LocalAccountCrypto.fromHex(preferences.getString("password_hash", "").orEmpty())
        val calculated = LocalAccountCrypto.passwordHash(candidate.toCharArray(), salt)
        LocalAccountCrypto.equalsConstantTime(stored, calculated)
    } catch (_: Exception) { false }

    companion object {
        fun validUsername(s: String): Boolean =
            s.length in 3..32 && s.all { it.isLetterOrDigit() || it in "._-" }

        fun validPassword(s: String): Boolean = s.length in 10..128
    }
}
