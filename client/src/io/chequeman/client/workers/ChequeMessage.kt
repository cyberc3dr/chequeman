package io.chequeman.client.workers

import io.chequeman.models.SourceContainer

data class ChequeMessage(
    val text: String,
    val buttonText: String,
    val link: String,
    val source: SourceContainer
)
