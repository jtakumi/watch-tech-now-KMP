package com.jtakumi.watchtechnow.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

private const val MAX_RETRY_ATTEMPTS = 2

fun createHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )
        }
        install(HttpRequestRetry) {
            maxRetries = MAX_RETRY_ATTEMPTS
            retryIf { _, response ->
                response.status == HttpStatusCode.TooManyRequests ||
                    response.status.value >= HttpStatusCode.InternalServerError.value
            }
            retryOnExceptionIf { _, cause -> cause is IOException }
            exponentialDelay()
        }
    }
    return if (engine == null) HttpClient(configure) else HttpClient(engine, configure)
}

expect fun createPlatformHttpClient(): HttpClient
