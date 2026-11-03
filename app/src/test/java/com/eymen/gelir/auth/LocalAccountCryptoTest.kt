package com.eymen.gelir.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAccountCryptoTest {
    @Test fun passwordHashUsesSaltAndVerifiesCorrectly() {
        val first = LocalAccountCrypto.passwordHash("TestPass2026!".toCharArray(), ByteArray(16) { 1 })
        val same = LocalAccountCrypto.passwordHash("TestPass2026!".toCharArray(), ByteArray(16) { 1 })
        val different = LocalAccountCrypto.passwordHash("TestPass2026!".toCharArray(), ByteArray(16) { 2 })
        assertTrue(LocalAccountCrypto.equalsConstantTime(first, same))
        assertFalse(LocalAccountCrypto.equalsConstantTime(first, different))
    }

    @Test fun recoveryCodesHaveCorrectFormatAndDigestIsNormalized() {
        val code = LocalAccountCrypto.newRecoveryCode()
        assertEquals(29, code.length)
        val salt = ByteArray(16) { 3 }
        val hash1 = LocalAccountCrypto.recoveryDigest(code, salt)
        val hash2 = LocalAccountCrypto.recoveryDigest(code.replace("-", "").lowercase(), salt)
        assertEquals(hash1, hash2)
    }

    @Test fun numberedAndUnicodeSeparatedCodePastesAreAccepted() {
        val code = "AB12-CD34-EF56-7890-ABCD-EF12"
        val salt = ByteArray(16) { 7 }
        val digest = LocalAccountCrypto.recoveryDigest(code, salt)
        assertEquals(digest, LocalAccountCrypto.recoveryDigest("1.  " + code, salt))
        assertEquals(digest, LocalAccountCrypto.recoveryDigest("2) " + code, salt))
        assertEquals(digest, LocalAccountCrypto.recoveryDigest(code.replace("-", "–"), salt))
        assertEquals(digest, LocalAccountCrypto.recoveryDigest(code.replace("-", " "), salt))
        assertEquals(digest, LocalAccountCrypto.recoveryDigest(code.lowercase(), salt))
    }

    @Test fun wrongCodeStillProducesDifferentDigest() {
        val salt = ByteArray(16) { 4 }
        val good = LocalAccountCrypto.recoveryDigest("AB12-CD34-EF56-7890-ABCD-EF12", salt)
        val bad = LocalAccountCrypto.recoveryDigest("AB12-CD34-EF56-7890-ABCD-EF13", salt)
        assertFalse(LocalAccountCrypto.equalsConstantTime(LocalAccountCrypto.fromHex(good), LocalAccountCrypto.fromHex(bad)))
    }

    @Test fun temporaryPasswordNotEqualRecoveryCode() {
        val temp = LocalAccountCrypto.newTemporaryPassword()
        assertEquals(39, temp.length)
        assertFalse(LocalAccountCrypto.canonicalRecoveryCode(temp).length == 24)
    }
}
