package com.jtakumi.watchtechnow

import androidx.compose.ui.window.ComposeUIViewController
import com.jtakumi.watchtechnow.di.initKoin

private val koinApplication by lazy(::initKoin)

fun MainViewController() = koinApplication.let {
    ComposeUIViewController { App() }
}
