package com.jtakumi.watchtechnow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.jtakumi.watchtechnow.domain.Article
import com.jtakumi.watchtechnow.domain.ArticlePage
import com.jtakumi.watchtechnow.domain.ArticleRepository
import com.jtakumi.watchtechnow.domain.ArticleSource
import com.jtakumi.watchtechnow.presentation.ArticlesViewModel
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class ArticlesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun articleDetailsAreDisplayed() {
        val viewModel = ArticlesViewModel(
            repository = object : ArticleRepository {
                override suspend fun getArticles(cursor: String?, query: String?) = ArticlePage(
                    articles = listOf(
                        Article(
                            id = "q1",
                            source = ArticleSource.QIITA,
                            title = "Compose Multiplatform入門",
                            url = "https://qiita.com/example/items/q1",
                            authorName = "Alice",
                            authorAvatarUrl = null,
                            likeCount = 12,
                            publishedAt = Instant.parse("2026-07-01T00:00:00Z"),
                        ),
                    ),
                    nextCursor = null,
                )
            },
        )

        composeRule.setContent { ArticlesScreen(viewModel) }

        composeRule.onNodeWithTag("article-QIITA-q1").assertIsDisplayed()
        composeRule.onNodeWithText("Compose Multiplatform入門").assertIsDisplayed()
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("♥ 12").assertIsDisplayed()
        viewModel.close()
    }
}
