package com.arjun.firechat.model

data class Message(
    var id: String = "",
    var message: String = "",
    var seen: Boolean = false,
    var type: String = "",
    var timestamp: Long = 0,
    var from: String = ""
)