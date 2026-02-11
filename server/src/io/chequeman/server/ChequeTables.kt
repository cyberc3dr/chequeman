package io.chequeman.server

import io.chequeman.models.ChequeContainer
import io.chequeman.models.SourceContainer
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChequeRepository : JpaRepository<Cheque, String>

@Entity
@Table(name = "cheques")
data class Cheque(
    val bot: String,

    val ids: List<String>,

    @JdbcTypeCode(SqlTypes.JSON)
    val sources: List<SourceContainer>,

    @JdbcTypeCode(SqlTypes.JSON)
    val cheque: ChequeContainer?,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null
)