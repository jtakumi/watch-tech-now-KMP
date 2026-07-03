package com.jtakumi.watchtechnow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jtakumi.watchtechnow.presentation.ArticlesViewModel
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
private fun ArticlesScreen(viewModel: ArticlesViewModel = koinInject()) {
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.articles, key = { "${it.source}:${it.id}" }) { article ->
                    Column {
                        Text(article.title, style = MaterialTheme.typography.titleMedium)
                        Text("${article.source} · ${article.authorName} · ♥ ${article.likeCount}")
                    }
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
