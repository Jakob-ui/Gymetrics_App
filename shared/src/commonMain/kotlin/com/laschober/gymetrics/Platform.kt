package com.laschober.gymetrics

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
