package app.rtmt.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordEncoder {
    private val encoder = BCryptPasswordEncoder()
    fun hash(raw: String) = encoder.encode(raw)
    fun matches(raw: String, hashed: String) = encoder.matches(raw, hashed)
}
