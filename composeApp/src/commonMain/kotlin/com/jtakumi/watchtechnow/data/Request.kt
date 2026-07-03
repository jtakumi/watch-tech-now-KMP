package com.jtakumi.watchtechnow.data

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

suspend inline fun <reified T> apiRequest(request: () -> HttpResponse): T =
    try {
        request().body()
    } catch (exception: ClientRequestException) {
        if (exception.response.status.value == 429) throw ApiException.RateLimited()
        throw ApiException.Unexpected(exception.response.status.value)
    } catch (exception: ServerResponseException) {
        throw ApiException.Server(exception.response.status.value)
    } catch (exception: ContentConvertException) {
        throw ApiException.InvalidResponse(exception)
    } catch (exception: SerializationException) {
        throw ApiException.InvalidResponse(exception)
    } catch (exception: IOException) {
        throw ApiException.Network(exception)
    }
