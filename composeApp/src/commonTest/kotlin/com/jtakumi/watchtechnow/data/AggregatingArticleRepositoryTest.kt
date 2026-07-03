package com.jtakumi.watchtechnow.data

import com.jtakumi.watchtechnow.domain.ArticlePage
import com.jtakumi.watchtechnow.domain.ArticleRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AggregatingArticleRepositoryTest {
    @Test
    fun oneSourceFailureDoesNotPreventTheOtherSource() = runTest {
        val failing = StubRepository { _, _ -> error("Qiita unavailable") }
        val available = StubRepository { _, _ -> ArticlePage(emptyList(), "2") }
        val repository = AggregatingArticleRepository(failing, available)

        val page = repository.getArticles()

        assertNotNull(page.nextCursor)
        assertEquals(1, failing.calls)
        assertEquals(1, available.calls)
    }

    @Test
    fun completedSourceIsNotRestartedOnTheNextCombinedPage() = runTest {
        val completed = StubRepository { _, _ -> ArticlePage(emptyList(), null) }
        val paged = StubRepository { cursor, _ ->
            ArticlePage(emptyList(), if (cursor == null) "2" else null)
        }
        val repository = AggregatingArticleRepository(completed, paged)

        val first = repository.getArticles()
        repository.getArticles(first.nextCursor)

        assertEquals(1, completed.calls)
        assertEquals(2, paged.calls)
    }

    private class StubRepository(
        private val response: suspend (String?, String?) -> ArticlePage,
    ) : ArticleRepository {
        var calls = 0

        override suspend fun getArticles(cursor: String?, query: String?): ArticlePage {
            calls += 1
            return response(cursor, query)
        }
    }
}
