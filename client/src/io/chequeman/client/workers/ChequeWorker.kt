package io.chequeman.client.workers

import io.chequeman.models.ChequeContainer
import io.chequeman.models.ChequeModel

interface ChequeWorker {

    fun parse(raw: ChequeModel): ChequeMessage?
}