import java.security.MessageDigest
import java.util.Base64

fun main() {
    val password = "default123"
    val salt = "salt"
    val saltedPassword = password + salt
    val hash = sha256(saltedPassword)
    val hashedPassword = "$salt:$hash"
    
    println("Password: $password")
    println("Salt: $salt")
    println("Salted Password: $saltedPassword")
    println("Hash: $hash")
    println("Hashed Password: $hashedPassword")
    
    // Test verification
    val isValid = verifyPassword(password, hashedPassword)
    println("Verification test: $isValid")
}

fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(input.toByteArray())
    return Base64.getEncoder().encodeToString(hash)
}

fun verifyPassword(password: String, hashedPassword: String): Boolean {
    val parts = hashedPassword.split(":")
    if (parts.size != 2) return false
    
    val salt = parts[0]
    val hash = parts[1]
    val saltedPassword = password + salt
    val computedHash = sha256(saltedPassword)
    
    println("Verification details:")
    println("  Stored salt: $salt")
    println("  Stored hash: $hash")
    println("  Computed salted password: $saltedPassword")
    println("  Computed hash: $computedHash")
    println("  Hashes match: ${hash == computedHash}")
    
    return hash == computedHash
}
