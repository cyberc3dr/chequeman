package io.chequeman.client.workers

val Number.plainString: String
    get() = toFloat().toBigDecimal().stripTrailingZeros().toPlainString()