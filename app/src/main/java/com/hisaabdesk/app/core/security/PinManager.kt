
package com.hisaabdesk.app.core.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.prefs by preferencesDataStore(name = "hisaabdesk_settings")

/** PIN stored ONLY as PBKDF2-HMAC-SHA256 hash + random salt. Never plaintext. */
class PinManager(private val context: Context) {
    private val keyHash = stringPreferencesKey("pin_hash")
    private val keySalt = stringPreferencesKey("pin_salt")
    private val keyBio = stringPreferencesKey("biometric_enabled")
    private val keyLock = stringPreferencesKey("lock_timeout_sec")
    private val keyOnboarded = stringPreferencesKey("onboarded")

    val isPinSet: Flow<Boolean> = context.prefs.data.map { it[keyHash] != null }
    val biometricEnabled: Flow<Boolean> = context.prefs.data.map { it[keyBio] == "1" }
    val onboarded: Flow<Boolean> = context.prefs.data.map { it[keyOnboarded] == "1" }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
    suspend fun setPin(pin: String) {
        require(pin.length in 4..6 && pin.all { it.isDigit() }) { "পিন ৪–৬ সংখ্যার হতে হবে।" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val h = hash(pin, salt)
        context.prefs.edit { it[keySalt] = Base64.getEncoder().encodeToString(salt); it[keyHash] = Base64.getEncoder().encodeToString(h); it[keyOnboarded] = "1" }
    }
    suspend fun verify(pin: String): Boolean {
        val p = context.prefs.data.first()
        val saltB = p[keySalt] ?: return false; val hashB = p[keyHash] ?: return false
        val expected = Base64.getDecoder().decode(hashB)
        val actual = hash(pin, Base64.getDecoder().decode(saltB))
        return MessageDigest.isEqual(expected, actual)
    }
    suspend fun setBiometric(enabled: Boolean) { context.prefs.edit { it[keyBio] = if (enabled) "1" else "0" } }
    suspend fun exportSettings(): Map<String, String> = mapOf("biometric" to (if (biometricEnabled.first()) "1" else "0"))
}
