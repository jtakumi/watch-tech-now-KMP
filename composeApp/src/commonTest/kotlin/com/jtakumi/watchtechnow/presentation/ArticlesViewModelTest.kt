package com.jtakumi.watchtechnow.presentation

import com.jtakumi.watchtechnow.data.ApiException
import com.jtakumi.watchtechnow.domain.ArticlePage
import com.jtakumi.watchtechnow.domain.ArticleRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticlesViewModelTest {
    @Test
    fun rateLimitHasActionableMessage() = runTest {
        val viewModel = ArticlesViewModel(
            repository = object : ArticleRepository {
                override suspend fun getArticles(cursor: String?, query: String?): ArticlePage {
                    throw ApiException.RateLimited()
                }
            },
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
}
