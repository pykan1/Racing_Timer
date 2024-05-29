package com.example.racing.ext

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.formatTimestampToDateTimeString(): String {
    val date = Date(this)
    val format = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    return format.format(date)
}

fun Long.formatSeconds(returnEmptyIfZero: Boolean = false): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val remainingSeconds = this % 60
    val result = String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    return if (returnEmptyIfZero && this == 0.toLong()) {
        ""
    } else result
}

fun getCurrentTimeInMillis(): Long {
    return System.currentTimeMillis()
}