package io.chequeman.server.auth

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.security.Principal

@Entity
@Table(name = "users")
data class User(
    val username: String,

    val token: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
) : Principal {
    override fun getName() = username
}

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByToken(token: String): User?
}