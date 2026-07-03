package com.jtakumi.watchtechnow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jtakumi.watchtechnow.presentation.ArticlesViewModel
import com.jtakumi.watchtechnow.domain.Article
import coil3.compose.AsyncImage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

private const val ARTICLES_ROUTE = "articles"

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = ARTICLES_ROUTE) {
            composable(ARTICLES_ROUTE) {
                ArticlesScreen()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ArticlesScreen(viewModel: ArticlesViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.loadInitial()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Watch Tech Now") }) }) { padding ->
        when {
            state.isLoading && state.articles.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            state.errorMessage != null && state.articles.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.errorMessage.orEmpty())
                Button(onClick = viewModel::retry) { Text("再試行") }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).testTag("article-list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.articles, key = { "${it.source}:${it.id}" }) { article ->
                    ArticleCard(article)
                }
                if (state.canLoadMore) {
                    item {
                        Button(
                            enabled = !state.isLoading,
                            onClick = viewModel::loadMore,
                        ) {
                            Text(if (state.isLoading) "読み込み中…" else "さらに読み込む")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: Article) {
    Card(modifier = Modifier.fillMaxWidth().testTag("article-${article.source}-${article.id}")) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(article.emoji ?: article.source.name, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(article.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = article.authorAvatarUrl,
                    contentDescription = "${article.authorName}のアイコン",
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(article.authorName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        article.publishedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("♥ ${article.likeCount}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
