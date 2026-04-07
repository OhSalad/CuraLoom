package com.pmu.mobileapp.ui.common

fun titleFromUrl(url: String): String {
    return try {
        val clean = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
        val host = clean.substringBefore("/").substringBefore(".")
        host.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " Feed"
    } catch (_: Exception) {
        "New Feed"
    }
}
