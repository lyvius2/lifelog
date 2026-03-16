package com.walter.lifelog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication(scanBasePackages = ["com.walter.lifelog"])
@EnableCaching
class LifelogApplication

fun main(args: Array<String>) {
    runApplication<LifelogApplication>(*args)
}
