package com.jtakumi.watchtechnow.presentation

import com.jtakumi.watchtechnow.domain.Article
import com.jtakumi.watchtechnow.domain.ArticleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArticlesUiState(
    val articles: List<Article> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val errorMessage: String? = null,
)

class ArticlesViewModel(
    private val repository: ArticleRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow(ArticlesUiState())
    val state: StateFlow<ArticlesUiState> = mutableState.asStateFlow()
    private var nextCursor: String? = null
    private var loadJob: Job? = null

    fun loadInitial(query: String = mutableState.value.query) {
        nextCursor = null
        mutableState.update { it.copy(articles = emptyList(), query = query, canLoadMore = true) }
        load(append = false)
    }

    fun loadMore() {
        if (mutableState.value.canLoadMore) load(append = true)
    }

    fun retry() {
        load(append = mutableState.value.articles.isNotEmpty())
    }

    private fun load(append: Boolean) {
        if (loadJob?.isActive == true) return
        loadJob = scope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getArticles(
                    cursor = if (append) nextCursor else null,
                    query = mutableState.value.query.takeIf(String::isNotBlank),
                )
            }.onSuccess { page ->
                nextCursor = page.nextCursor
                mutableState.update {
                    it.copy(
                        articles = if (append) it.articles + page.articles else page.articles,
                        isLoading = false,
                        canLoadMore = page.nextCursor != null,
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "記事を取得できませんでした")
                }
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
