package com.example.cci.core.security

import com.example.cci.core.constants.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {
    private const val SALT_BYTES = 16

    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val saltHex = salt.toHex()
        val digest = sha256("$saltHex:$password")
        return "$saltHex:$digest"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val saltHex = parts[0]
        val expected = parts[1]
        val actual = sha256("$saltHex:$password")
        return MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}

data class ActiveSession(
    val userId: Long,
    val username: String,
    val fullName: String,
    val role: UserRole
)

class SessionManager {
    private val _currentSession = MutableStateFlow(
        ActiveSession(
            userId = 0L,
            username = "shiftincharge",
            fullName = "Sinter Shift In-Charge",
            role = UserRole.SHIFT_IN_CHARGE
        )
    )
    val currentSession: StateFlow<ActiveSession> = _currentSession.asStateFlow()

    fun setSession(session: ActiveSession) {
        _currentSession.value = session
    }

    fun currentUsername(): String = _currentSession.value.username
    fun currentRole(): UserRole = _currentSession.value.role
}
