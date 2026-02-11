package io.chequeman.server.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val userRepository: UserRepository,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity) : SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/read/**", "/ws").permitAll()
                .anyRequest().authenticated()
        }
        .addFilterBefore(TokenAuthenticationFilter(userRepository),
            UsernamePasswordAuthenticationFilter::class.java)
        .build()
}