package app.rtmt.api

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
    private val users: UserRepository
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
        // 데모 단계: 다음 단계에서 BCrypt로 교체
        val u = User(email = req.email, password_hash = "SHA256:${req.password}", nick = req.nick)
        return users.save(u)
    }
}
