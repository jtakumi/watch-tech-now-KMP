package com.jtakumi.watchtechnow.data

sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable) : ApiException("ネットワークに接続できません", cause)
    class RateLimited : ApiException("API の利用上限に達しました")
    class Server(val statusCode: Int) : ApiException("サーバーエラーが発生しました ($statusCode)")
    class Unexpected(val statusCode: Int) : ApiException("記事を取得できませんでした ($statusCode)")
    class InvalidResponse(cause: Throwable) : ApiException("API のレスポンスを解析できません", cause)
}
