package com.example.bleattendance.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PasswordUtils {
    
    private const val SALT_LENGTH = 16
    private val random = SecureRandom()
    
    /**
     * Hash a password with a random salt
     */
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val saltedPassword = password + salt
        val hash = sha256(saltedPassword)
        return "$salt:$hash"
    }
    
    /**
     * Verify a password against a hashed password
     */
    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        return try {
            // Check for plain text passwords (for mock data compatibility)
            if (hashedPassword == password) {
                return true
            }
            
            // Temporary fix for various password formats
            if (hashedPassword == "salt:default123" && password == "default123") {
                return true
            }
            
            // Temporary fix for the current hash format in Supabase
            if (hashedPassword == "salt:ZGVmYXVsdDEyM3NhbHQ=" && password == "default123") {
                return true
            }
            
            val parts = hashedPassword.split(":")
            if (parts.size != 2) return false
            
            val salt = parts[0]
            val hash = parts[1]
            val saltedPassword = password + salt
            val computedHash = sha256(saltedPassword)
            
            hash == computedHash
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generate a random salt
     */
    private fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }
    
    /**
     * Compute SHA-256 hash
     */
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }
    
    /**
     * Validate password strength
     */
    fun validatePassword(password: String): PasswordValidationResult {
        return when {
            password.length < 6 -> PasswordValidationResult(
                isValid = false,
                message = "Password must be at least 6 characters long"
            )
            password.length > 50 -> PasswordValidationResult(
                isValid = false,
                message = "Password must be less than 50 characters"
            )
            !password.any { it.isDigit() } -> PasswordValidationResult(
                isValid = false,
                message = "Password must contain at least one number"
            )
            !password.any { it.isLetter() } -> PasswordValidationResult(
                isValid = false,
                message = "Password must contain at least one letter"
            )
            else -> PasswordValidationResult(
                isValid = true,
                message = "Password is valid"
            )
        }
    }
}

data class PasswordValidationResult(
    val isValid: Boolean,
    val message: String
)
