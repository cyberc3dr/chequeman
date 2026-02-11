package io.chequeman.client.http

import io.chequeman.models.ChequeContainer
import io.chequeman.models.ChequeModel
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

@Component
class RestChequeClient(
    @Value($$"${http.url}")
    private val httpUrl: String
) {

    private val restClient = RestClient.create(httpUrl)

    fun getCheque(id: UUID) : ChequeModel? = restClient.get()
        .uri("/api/read/cheques/{id}", id)
        .retrieve()
        .body(ChequeModel::class.java)
}