package io.chequeman.server.auth

import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class TokenCommands(
    private val tokenUtil: TokenUtil,
    private val userRepository: UserRepository
) {

    @ShellMethod("Generate a new token for a user")
    fun generateToken(
        @ShellOption username: String
    ): String {
        val token = tokenUtil.generateToken()

        val user = User(
            username = username,
            token = token
        )

        userRepository.save(user)

        return "Created user '$username' with token: $token"
    }

    @ShellMethod("Rotate a user's token")
    fun rotateToken(
        @ShellOption username: String
    ): String {
        val user = userRepository.findAll().find { it.username == username }
            ?: return "User '$username' not found"

        val newToken = tokenUtil.generateToken()
        val updatedUser = user.copy(token = newToken)

        userRepository.save(updatedUser)

        return "Rotated token for user '$username'. New token: $newToken"
    }

    @ShellMethod("Delete a user by username")
    fun deleteUser(
        @ShellOption username: String
    ): String {
        val user = userRepository.findAll().find { it.username == username }
            ?: return "User '$username' not found"

        userRepository.delete(user)

        return "Deleted user '$username'"
    }
}