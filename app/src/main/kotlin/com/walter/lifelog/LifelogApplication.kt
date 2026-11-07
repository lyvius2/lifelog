package com.walter.lifelog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.walter.lifelog"])
@EnableCaching
@EnableScheduling
class LifelogApplication

fun main(args: Array<String>) {
    runApplication<LifelogApplication>(*args)
}
