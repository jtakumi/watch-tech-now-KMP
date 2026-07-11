package com.jtakumi.watchtechnow.presentation

import com.jtakumi.watchtechnow.data.ApiException
import com.jtakumi.watchtechnow.domain.Article
import com.jtakumi.watchtechnow.domain.ArticleRepository
import com.jtakumi.watchtechnow.domain.ArticleSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
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
    val source: ArticleSource? = null,
)

class ArticlesViewModel(
    private val repository: ArticleRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutableState = MutableStateFlow(ArticlesUiState())
    val state: StateFlow<ArticlesUiState> = mutableState.asStateFlow()
    private var nextCursor: String? = null
    private var loadJob: Job? = null

    fun loadInitial(
        query: String = mutableState.value.query,
        source: ArticleSource? = mutableState.value.source,
    ) {
        loadJob?.cancel()
        nextCursor = null
        mutableState.update {
            it.copy(
                articles = emptyList(),
                query = query.trim(),
                source = source,
                canLoadMore = true,
                errorMessage = null,
            )
        }
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
            try {
                val page = mutableState.value.source?.let { source ->
                    repository.getArticles(
                        source = source,
                        cursor = if (append) nextCursor else null,
                        query = mutableState.value.query.takeIf(String::isNotBlank),
                    )
                } ?: repository.getArticles(
                    cursor = if (append) nextCursor else null,
                    query = mutableState.value.query.takeIf(String::isNotBlank),
                )
                page.let { page ->
                nextCursor = page.nextCursor
                mutableState.update {
                    val articles = if (append) it.articles + page.articles else page.articles
                    it.copy(
                        articles = articles.distinctBy { article -> article.source to article.id },
                        isLoading = false,
                        canLoadMore = page.nextCursor != null,
                    )
                }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(isLoading = false, errorMessage = error.toUserMessage())
                }
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}

private fun Throwable.toUserMessage(): String = when (this) {
    is ApiException.RateLimited -> "アクセスが集中しています。しばらくしてから再試行してください"
    is ApiException.Network -> "通信に失敗しました。接続を確認して再試行してください"
    else -> message ?: "記事を取得できませんでした"
}
