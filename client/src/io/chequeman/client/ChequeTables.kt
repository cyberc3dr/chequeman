package io.chequeman.client

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChequeRepository : JpaRepository<Cheque, String> {
    fun findByUuid(uuid: UUID): Cheque?
}

@Entity
@Table(name = "cheque_messages")
data class Cheque(
    @Id
    val uuid: UUID,

    val messageId: Int
)