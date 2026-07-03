package com.jtakumi.watchtechnow.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClient(): HttpClient = createHttpClient(Darwin.create())
