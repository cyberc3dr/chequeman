package io.chequeman.models

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.*

data class ChequeWriteRequest(
    val bot: String,
    val link: String,
    val source: String,
    val sourceTitle: String,
    val message: String? = null,
    val markdownMessage: String? = null,
    val forwardMessage: String? = null,
    val forwardMarkdownMessage: String? = null,
    val inlineTitle: String? = null,
    val inlineDescription: String? = null,
    val buttonText: String? = null,
)

data class ChequeUpdate(
    val uuid: UUID = UUID.randomUUID()
)

data class ChequeModel(
    val bot: String,
    val ids: List<String>,
    val sources: List<SourceContainer>,
    val cheque: ChequeContainer?,
    val uuid: UUID = UUID.randomUUID()
)

data class SourceContainer(
    val title: String,
    val link: String
)

data class ChequeEntry(
    val id: String,
    val source: SourceContainer,
    val cheque: ChequeContainer?
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface ChequeContainer {
    val oneActivation: Double
    val currency: String

    // Безопасное копирование данных из старого чека в новый
    fun safeCopyFrom(new: ChequeContainer): ChequeContainer = new
}

@JsonTypeName("xrocket_mc")
data class XRocketMC(
    override val oneActivation: Double,
    override val currency: String,
    val picture: String? = null,
    val description: String? = null,
    val totalAmount: Double,
    val activations: Int,
    val referralPercent: Int = 0,
    val isPremium: Boolean = false,
    val password: String? = null,
    val groups: List<String> = emptyList(),
    val awardsPaid: Int? = null,
    val old: Boolean = false
) : ChequeContainer {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XRocketMC) return false

        if (oneActivation != other.oneActivation) return false
        if (currency != other.currency) return false
        if (description != other.description) return false
        if (totalAmount != other.totalAmount) return false
        if (activations != other.activations) return false
        if (referralPercent != other.referralPercent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = oneActivation.hashCode()
        result = 31 * result + totalAmount.hashCode()
        result = 31 * result + activations
        result = 31 * result + referralPercent
        result = 31 * result + currency.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        return result
    }

    // Копируем важные поля из старого чека, если они не заданы в новом
    override fun safeCopyFrom(new: ChequeContainer): XRocketMC {
        if (new !is XRocketMC) return this
        return new.copy(
            password = new.password ?: this.password,
            groups = new.groups.ifEmpty { this.groups },
            awardsPaid = new.awardsPaid ?: this.awardsPaid
        )
    }
}

@JsonTypeName("xrocket_personal")
data class XRocketPersonal(
    override val oneActivation: Double,
    override val currency: String,
    val old: Boolean = false
) : ChequeContainer

@JsonTypeName("send_mc")
data class CryptoBotMC(
    override val currency: String,
    override val oneActivation: Double,
    val picture: String? = null,
    val description: String? = null,
    val totalAmount: Double,
    val activations: Int,
    val activated: Int? = null,
    val passwordProtected: Boolean = false,
    val isPremium: Boolean = false,
    val requireNewUser: Boolean = false,
) : ChequeContainer {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CryptoBotMC) return false

        if (oneActivation != other.oneActivation) return false
        if (currency != other.currency) return false
        if (description != other.description) return false
        if (totalAmount != other.totalAmount) return false
        if (activations != other.activations) return false

        return true
    }

    override fun hashCode(): Int {
        var result = oneActivation.hashCode()
        result = 31 * result + totalAmount.hashCode()
        result = 31 * result + activations
        result = 31 * result + currency.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        return result
    }

    override fun safeCopyFrom(new: ChequeContainer): CryptoBotMC {
        if (new !is CryptoBotMC) return this
        return new.copy(
            activated = new.activated ?: this.activated,
            passwordProtected = new.passwordProtected || this.passwordProtected,
            isPremium = new.isPremium || this.isPremium,
            requireNewUser = new.requireNewUser || this.requireNewUser
        )
    }
}

@JsonTypeName("send_personal")
data class CryptoBotPersonal(
    override val oneActivation: Double,
    override val currency: String,
    val receiver: String? = null,
    val hasPassword: Boolean = false
) : ChequeContainer {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CryptoBotPersonal) return false

        if (oneActivation != other.oneActivation) return false
        if (currency != other.currency) return false
        if (receiver != other.receiver) return false

        return true
    }

    override fun hashCode(): Int {
        var result = oneActivation.hashCode()
        result = 31 * result + currency.hashCode()
        result = 31 * result + receiver.hashCode()
        return result
    }
}
