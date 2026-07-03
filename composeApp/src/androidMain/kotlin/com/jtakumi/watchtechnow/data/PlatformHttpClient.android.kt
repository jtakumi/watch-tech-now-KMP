package com.jtakumi.watchtechnow.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createPlatformHttpClient(): HttpClient = createHttpClient(OkHttp.create())
