package com.jtakumi.watchtechnow.data.zenn

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

private const val ZENN_ARTICLES_URL = "https://zenn.dev/api/articles"
private const val ZENN_SEARCH_URL = "https://zenn.dev/api/search"

class ZennArticleRepository(
    private val client: HttpClient,
) : ArticleRepository {
    override suspend fun getArticles(cursor: String?, query: String?): ArticlePage {
        val page = cursor?.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val response: ZennArticlesDto = apiRequest {
            if (query.isNullOrBlank()) {
                client.get(ZENN_ARTICLES_URL) {
                    parameter("order", "latest")
                    parameter("page", page)
                }
            } else {
                client.get(ZENN_SEARCH_URL) {
                    parameter("q", query)
                    parameter("page", page)
                }
            }
        }
        return ArticlePage(
            articles = response.articles.map(ZennArticleDto::toArticle),
            nextCursor = response.nextPage?.toString(),
        )
    }
}

@Serializable
private data class ZennArticlesDto(
    val articles: List<ZennArticleDto> = emptyList(),
    @SerialName("next_page") val nextPage: Int? = null,
)

@Serializable
private data class ZennArticleDto(
    val id: Int,
    val slug: String,
    val title: String,
    val emoji: String? = null,
    @SerialName("liked_count") val likedCount: Int = 0,
    @SerialName("published_at") val publishedAt: String,
    val user: ZennUserDto,
) {
    fun toArticle() = Article(
        id = id.toString(),
        source = ArticleSource.ZENN,
        title = title,
        url = "https://zenn.dev/${user.username}/articles/$slug",
        authorName = user.name.ifBlank { user.username },
        authorAvatarUrl = user.avatarSmallUrl,
        likeCount = likedCount,
        publishedAt = Instant.parse(publishedAt),
        emoji = emoji,
    )
}

@Serializable
private data class ZennUserDto(
    val username: String,
    val name: String = "",
    @SerialName("avatar_small_url") val avatarSmallUrl: String? = null,
)
