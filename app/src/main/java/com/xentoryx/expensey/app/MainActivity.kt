package com.xentoryx.expensey.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.xentoryx.expensey.app.navigation.RootNavigation
import com.xentoryx.expensey.app.ui.theme.ExpenseyTheme
import com.xentoryx.expensey.core.storage.TokenManager
import org.koin.compose.koinInject

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge transparent system bars
        enableEdgeToEdge()

        setContent {
            val tokenManager: TokenManager = koinInject()
            val themeMode by tokenManager.themeMode.collectAsState(initial = "system")

            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            ExpenseyTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    RootNavigation(navController = navController)
                }
            }
        }
    }
}