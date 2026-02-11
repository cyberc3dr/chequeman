package io.chequeman.client

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ClientApplication

fun main(args: Array<String>) {
    runApplication<ClientApplication>(*args)
}