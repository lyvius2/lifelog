package com.walter.lifelog.worker.config

interface JpaProperties {
    var ddlAuto: String
    var databasePlatform: String
    var showSql: Boolean
    var formatSql: Boolean
}
