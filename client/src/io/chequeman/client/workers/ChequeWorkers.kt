package io.chequeman.client.workers

import io.chequeman.client.workers.xrocket.XRMCWorker
import io.chequeman.client.workers.xrocket.XRocketPersonalWorker
import io.chequeman.client.workers.cryptobot.CryptoBotMCWorker
import io.chequeman.client.workers.cryptobot.CryptoBotPersonalWorker
import io.chequeman.models.ChequeModel
import io.chequeman.models.XRocketMC
import io.chequeman.models.XRocketPersonal
import io.chequeman.models.CryptoBotMC
import io.chequeman.models.CryptoBotPersonal
import org.springframework.stereotype.Component

@Component
class ChequeWorkers {

    private val map = mutableMapOf(
        XRocketMC::class to XRMCWorker,
        XRocketPersonal::class to XRocketPersonalWorker,
        CryptoBotMC::class to CryptoBotMCWorker,
        CryptoBotPersonal::class to CryptoBotPersonalWorker
    )

    fun parse(raw: ChequeModel): ChequeMessage? {
        val cheque = raw.cheque

        if(cheque != null) {
            return map[cheque::class]?.parse(raw)
        }

        if(raw.bot.lowercase() in listOf("send", "cryptobot")) {
            return ChequeMessage(
                text = "**CryptoBot** | **Информация недоступна**\n\n#CryptoBot #Ссылка",
                buttonText = "\uD83D\uDC8E Активировать чек",
                link = "https://t.me/send?start=${raw.ids.first()}",
                source = raw.sources.first()
            )
        }

        return null
    }
}