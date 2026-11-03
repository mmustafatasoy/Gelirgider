package com.eymen.gelir.auth

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Exercises the actual Android SharedPreferences account and recovery workflow. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalAccountStoreTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before fun resetTestPrefs() {
        context.getSharedPreferences("eymen_local_account_v1", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun numberedCopiedRecoveryCodeWorksAndIsSingleUse() {
        val store = LocalAccountStore(context)
        val codes = store.register("Aile2026", "OldPassword-2026!")
        assertEquals(8, codes.size)
        assertTrue(store.authenticate("Aile2026", "OldPassword-2026!"))

        // Exactly what a user may copy from the original screen.
        val temporary = store.redeemRecovery("Aile2026", "1.  " + codes[0])
        assertNotNull(temporary)
        assertTrue(store.mustChangeTemporaryPassword())
        assertFalse(store.authenticate("Aile2026", "OldPassword-2026!"))
        assertNull(store.redeemRecovery("Aile2026", codes[0]))
        assertTrue(store.replaceTemporaryPassword(temporary!!, "NewPassword-2026!"))
        assertFalse(store.mustChangeTemporaryPassword())
        assertFalse(store.authenticate("Aile2026", "OldPassword-2026!"))
        assertTrue(store.authenticate("Aile2026", "NewPassword-2026!"))
    }

    @Test fun validCodeWithWrongUsernameFailsWithoutConsumingIt() {
        val store = LocalAccountStore(context)
        val codes = store.register("Kullanici", "MySafePassword-2026")
        assertNull(store.redeemRecovery("baska", codes[0]))
        assertNotNull(store.redeemRecovery("Kullanici", codes[0]))
    }

    @Test fun regeneratingCodesInvalidatesOlderCodes() {
        val store = LocalAccountStore(context)
        val first = store.register("Kullanici", "MySafePassword-2026")
        val next = store.regenerateRecoveryCodes("MySafePassword-2026")
        assertNotNull(next)
        assertEquals(8, next!!.size)
        assertNull(store.redeemRecovery("Kullanici", first[0]))
        assertNotNull(store.redeemRecovery("Kullanici", next[0]))
    }
}
