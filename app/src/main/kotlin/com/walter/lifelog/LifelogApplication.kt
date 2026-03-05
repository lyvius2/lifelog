package com.walter.lifelog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.walter.lifelog"])
class LifelogApplication

fun main(args: Array<String>) {
    runApplication<LifelogApplication>(*args)
}
