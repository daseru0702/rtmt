package app.rtmt.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("users")
data class User(
    @Id val id: Long? = null,
    val email: String,
    val password_hash: String,
    val nick: String,
    val rating: Int = 1000
)
