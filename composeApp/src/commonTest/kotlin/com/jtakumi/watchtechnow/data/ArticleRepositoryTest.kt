package com.jtakumi.watchtechnow.data

import com.jtakumi.watchtechnow.data.qiita.QiitaArticleRepository
import com.jtakumi.watchtechnow.data.zenn.ZennArticleRepository
import com.jtakumi.watchtechnow.domain.ArticleSource
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ArticleRepositoryTest {
    @Test
    fun qiitaResponseIsMappedToArticle() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    [{
                      "id":"q1","title":"KMP","url":"https://qiita.com/a/items/q1",
                      "likes_count":12,"created_at":"2026-07-01T00:00:00Z",
                      "rendered_body":"<p>body</p>",
                      "user":{"id":"a","name":"Alice","profile_image_url":"https://example.com/a.png"}
                    }]
                """.trimIndent(),
                headers = jsonHeaders,
            )
        }

        val page = QiitaArticleRepository(createHttpClient(engine)).getArticles()

        assertEquals(1, page.articles.size)
        assertEquals(ArticleSource.QIITA, page.articles.single().source)
        assertEquals("Alice", page.articles.single().authorName)
        assertEquals("<p>body</p>", page.articles.single().renderedBody)
        assertNull(page.nextCursor)
    }

    @Test
    fun zennNextPageIsExposedAsCommonCursor() = runTest {
        val engine = MockEngine { request ->
            assertEquals("4", request.url.parameters["page"])
            respond(
                content = """
                    {
                      "articles":[{
                        "id":7,"slug":"hello-kmp","title":"Hello","emoji":"🧭",
                        "liked_count":3,"published_at":"2026-07-02T00:00:00Z",
                        "user":{"username":"bob","name":"","avatar_small_url":null}
                      }],
                      "next_page":5
                    }
                """.trimIndent(),
                headers = jsonHeaders,
            )
        }

        val page = ZennArticleRepository(createHttpClient(engine)).getArticles(cursor = "4")

        assertEquals("5", page.nextCursor)
        assertEquals("bob", page.articles.single().authorName)
        assertEquals("https://zenn.dev/bob/articles/hello-kmp", page.articles.single().url)
    }

    @Test
    fun httpFailureIsConvertedToDomainError() = runTest {
        val engine = MockEngine {
            respond("unavailable", HttpStatusCode.BadRequest)
        }

        assertFailsWith<ApiException.Unexpected> {
            QiitaArticleRepository(createHttpClient(engine)).getArticles()
        }
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
