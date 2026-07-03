package com.jtakumi.watchtechnow.data.qiita

import com.jtakumi.watchtechnow.data.apiRequest
import com.jtakumi.watchtechnow.domain.Article
import com.jtakumi.watchtechnow.domain.ArticlePage
import com.jtakumi.watchtechnow.domain.ArticleRepository
import com.jtakumi.watchtechnow.domain.ArticleSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val QIITA_ITEMS_URL = "https://qiita.com/api/v2/items"
private const val PAGE_SIZE = 20

class QiitaArticleRepository(
    private val client: HttpClient,
) : ArticleRepository {
    override suspend fun getArticles(cursor: String?, query: String?): ArticlePage {
        val page = cursor?.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val items: List<QiitaItemDto> = apiRequest {
            client.get(QIITA_ITEMS_URL) {
                parameter("page", page)
                parameter("per_page", PAGE_SIZE)
                query?.takeIf(String::isNotBlank)?.let { parameter("query", it) }
            }
        }
        return ArticlePage(
            articles = items.map(QiitaItemDto::toArticle),
            nextCursor = if (items.size == PAGE_SIZE) (page + 1).toString() else null,
        )
    }
}

@Serializable
private data class QiitaItemDto(
    val id: String,
    val title: String,
    val url: String,
    val user: QiitaUserDto,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("rendered_body") val renderedBody: String? = null,
) {
    fun toArticle() = Article(
        id = id,
        source = ArticleSource.QIITA,
        title = title,
        url = url,
        authorName = user.name.ifBlank { user.id },
        authorAvatarUrl = user.profileImageUrl,
        likeCount = likesCount,
        publishedAt = Instant.parse(createdAt),
        renderedBody = renderedBody,
    )
}

@Serializable
private data class QiitaUserDto(
    val id: String,
    val name: String = "",
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
)
