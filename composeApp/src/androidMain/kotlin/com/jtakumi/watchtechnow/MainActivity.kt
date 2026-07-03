package com.jtakumi.watchtechnow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jtakumi.watchtechnow.di.initKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isKoinInitialized) {
            initKoin()
            isKoinInitialized = true
        }
        setContent { App() }
    }

    private companion object {
        var isKoinInitialized = false
    }
}
