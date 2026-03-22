package com.walter.lifelog.worker.config

interface DatabaseProperties {
    var jdbcUrl: String
    var username: String
    var password: String
    var driverClassName: String
}