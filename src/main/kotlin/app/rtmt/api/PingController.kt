package app.rtmt.api

import app.rtmt.security.PasswordEncoder
import app.rtmt.user.User
import app.rtmt.user.UserRepository
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api")
class PingController(
    private val users: UserRepository,
    private val pw: PasswordEncoder
) {
    @GetMapping("/ping")
    fun ping() = mapOf("ok" to true)

    data class SignUpReq(
        @field:Email val email: String,
        @field:NotBlank val password: String,
        @field:NotBlank val nick: String
    )

    @PostMapping("/signup")
    suspend fun signup(@RequestBody req: SignUpReq): User {
        val u = User(
            email = req.email,
            password_hash = pw.hash(req.password),
            nick = req.nick
        )
        return users.save(u)
    }
}
