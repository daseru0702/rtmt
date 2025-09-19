package app.rtmt.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange { it.anyExchange().permitAll() } // 전부 허용 (개발용)
            .build()
}

// 나중에 로그인/JWT 붙일 때 여기서 /api/**는 인증 필요, /actuator/health만 permitAll 같은 식으로 정책 세분화 할 필요 있음.