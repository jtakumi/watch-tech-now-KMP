package com.jtakumi.watchtechnow.data

import com.jtakumi.watchtechnow.domain.ArticlePage
import com.jtakumi.watchtechnow.domain.ArticleRepository
import com.jtakumi.watchtechnow.domain.ArticleSource
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

private const val CURSOR_SEPARATOR = "|"

class AggregatingArticleRepository(
    private val qiita: ArticleRepository,
    private val zenn: ArticleRepository,
) : ArticleRepository {
    override suspend fun getArticles(
        source: ArticleSource,
        cursor: String?,
        query: String?,
    ): ArticlePage = when (source) {
        ArticleSource.QIITA -> qiita.getArticles(cursor, query)
        ArticleSource.ZENN -> zenn.getArticles(cursor, query)
    }

    override suspend fun getArticles(cursor: String?, query: String?): ArticlePage = supervisorScope {
        val (qiitaCursor, zennCursor) = decodeCursor(cursor)
        val qiitaActive = cursor == null || qiitaCursor != null
        val zennActive = cursor == null || zennCursor != null
        val qiitaPage = async {
            if (qiitaActive) runCatching { qiita.getArticles(qiitaCursor, query) }
            else Result.success(ArticlePage(emptyList(), null))
        }
        val zennPage = async {
            if (zennActive) runCatching { zenn.getArticles(zennCursor, query) }
            else Result.success(ArticlePage(emptyList(), null))
        }
        val results = listOf(qiitaPage.await(), zennPage.await())
        val successes = results.mapNotNull(Result<ArticlePage>::getOrNull)
        if (successes.isEmpty()) throw results.firstNotNullOf { it.exceptionOrNull() }

        val qiitaResult = results[0].getOrNull()
        val zennResult = results[1].getOrNull()
        ArticlePage(
            articles = successes.flatMap { it.articles }.sortedByDescending { it.publishedAt },
            nextCursor = encodeCursor(
                qiita = if (results[0].isFailure) qiitaCursor ?: "1" else qiitaResult?.nextCursor,
                zenn = if (results[1].isFailure) zennCursor ?: "1" else zennResult?.nextCursor,
            ),
        )
    }

    private fun decodeCursor(cursor: String?): Pair<String?, String?> {
        if (cursor == null) return null to null
        val parts = cursor.split(CURSOR_SEPARATOR, limit = 2)
        return parts.getOrNull(0).orEmpty().ifBlank { null } to
            parts.getOrNull(1).orEmpty().ifBlank { null }
    }

    private fun encodeCursor(qiita: String?, zenn: String?): String? =
        if (qiita == null && zenn == null) null else "${qiita.orEmpty()}$CURSOR_SEPARATOR${zenn.orEmpty()}"
}
