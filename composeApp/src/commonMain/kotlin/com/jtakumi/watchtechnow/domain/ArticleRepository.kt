package com.jtakumi.watchtechnow.domain

data class ArticlePage(
    val articles: List<Article>,
    val nextCursor: String?,
)

interface ArticleRepository {
    suspend fun getArticles(
        cursor: String? = null,
        query: String? = null,
    ): ArticlePage
}
