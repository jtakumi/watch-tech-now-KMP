package com.jtakumi.watchtechnow.di

import com.jtakumi.watchtechnow.data.AggregatingArticleRepository
import com.jtakumi.watchtechnow.data.createPlatformHttpClient
import com.jtakumi.watchtechnow.data.qiita.QiitaArticleRepository
import com.jtakumi.watchtechnow.data.zenn.ZennArticleRepository
import com.jtakumi.watchtechnow.domain.ArticleRepository
import com.jtakumi.watchtechnow.presentation.ArticlesViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val appModule = module {
    single { createPlatformHttpClient() }
    single<ArticleRepository>(named("qiita")) { QiitaArticleRepository(get()) }
    single<ArticleRepository>(named("zenn")) { ZennArticleRepository(get()) }
    single<ArticleRepository> {
        AggregatingArticleRepository(
            qiita = get(named("qiita")),
            zenn = get(named("zenn")),
        )
    }
    factory { ArticlesViewModel(get()) }
}

fun initKoin(): KoinApplication = startKoin {
    modules(appModule)
}
