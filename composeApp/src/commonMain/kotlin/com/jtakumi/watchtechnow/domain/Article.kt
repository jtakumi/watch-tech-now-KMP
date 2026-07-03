package com.jtakumi.watchtechnow.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: String,
    val source: ArticleSource,
    val title: String,
    val url: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val likeCount: Int,
    val publishedAt: Instant,
    val emoji: String? = null,
    val renderedBody: String? = null,
)

@Serializable
enum class ArticleSource {
    ZENN,
    QIITA,
}
