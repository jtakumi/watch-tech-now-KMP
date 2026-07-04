package com.jtakumi.watchtechnow.presentation

import com.jtakumi.watchtechnow.data.ApiException
import com.jtakumi.watchtechnow.domain.ArticlePage
import com.jtakumi.watchtechnow.domain.ArticleRepository
import com.jtakumi.watchtechnow.domain.Article
import com.jtakumi.watchtechnow.domain.ArticleSource
import kotlinx.datetime.Instant
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ArticlesViewModelTest {
    @Test
    fun rateLimitHasActionableMessage() = runTest {
        val viewModel = ArticlesViewModel(
            repository = object : ArticleRepository {
                override suspend fun getArticles(cursor: String?, query: String?): ArticlePage {
                    throw ApiException.RateLimited()
                }
            },
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        viewModel.loadInitial()
        while (viewModel.state.value.isLoading || viewModel.state.value.errorMessage == null) {
            testScheduler.advanceUntilIdle()
        }

        assertEquals(
            "アクセスが集中しています。しばらくしてから再試行してください",
            viewModel.state.value.errorMessage,
        )
        viewModel.close()
    }

    @Test
    fun appendDeduplicatesArticlesAndStopsAtEnd() = runTest {
        var calls = 0
        val article = article("same")
        val viewModel = ArticlesViewModel(
            repository = object : ArticleRepository {
                override suspend fun getArticles(cursor: String?, query: String?): ArticlePage {
                    calls += 1
                    return if (cursor == null) ArticlePage(listOf(article), "2")
                    else ArticlePage(listOf(article), null)
                }
            },
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        viewModel.loadInitial()
        testScheduler.advanceUntilIdle()
        viewModel.loadMore()
        testScheduler.advanceUntilIdle()

        assertEquals(2, calls)
        assertEquals(listOf(article), viewModel.state.value.articles)
        assertFalse(viewModel.state.value.canLoadMore)
        viewModel.close()
    }

    @Test
    fun failedAppendRetriesTheSameCursor() = runTest {
        val cursors = mutableListOf<String?>()
        var failOnce = true
        val viewModel = ArticlesViewModel(
            repository = object : ArticleRepository {
                override suspend fun getArticles(cursor: String?, query: String?): ArticlePage {
                    cursors += cursor
                    if (cursor == "2" && failOnce) {
                        failOnce = false
                        error("temporary")
                    }
                    return if (cursor == null) ArticlePage(listOf(article("1")), "2")
                    else ArticlePage(listOf(article("2")), null)
                }
            },
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        viewModel.loadInitial()
        testScheduler.advanceUntilIdle()
        viewModel.loadMore()
        testScheduler.advanceUntilIdle()
        viewModel.retry()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(null, "2", "2"), cursors)
        assertEquals(2, viewModel.state.value.articles.size)
        assertNull(viewModel.state.value.errorMessage)
        viewModel.close()
    }

    private fun article(id: String) = Article(
        id = id,
        source = ArticleSource.QIITA,
        title = id,
        url = "https://example.com/$id",
        authorName = "author",
        authorAvatarUrl = null,
        likeCount = 0,
        publishedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
