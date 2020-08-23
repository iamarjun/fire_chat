package com.arjun.firechat.model

import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Message(
    var id: String = "",
    var message: String = "",
    var seen: Boolean = false,
    var type: String = "",
    var timestamp: Long = 0,
    var from: String = ""
) {

    fun getTime(): String {
        val localTime =
            Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        val dateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        val time: String = localTime.format(dateTimeFormatter)
        Timber.d("Current Time $time")
        return time
    }
}