package io.chequeman.server.workers

import io.chequeman.models.ChequeContainer
import io.chequeman.models.ChequeEntry
import io.chequeman.models.ChequeWriteRequest

sealed interface Bot {

    val aliases: Set<String>

    fun handleRequest(request: ChequeWriteRequest) : ChequeEntry
}