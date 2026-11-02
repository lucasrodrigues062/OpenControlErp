package org.opencontrol.erp

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class OpenControlApplication

fun main(args: Array<String>) {
    SpringApplication.run(OpenControlApplication::class.java, *args)
}
