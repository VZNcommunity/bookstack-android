package com.vzith.bookstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.vzith.bookstack.ui.navigation.BookStackNavGraph
import com.vzith.bookstack.ui.navigation.Screen
import com.vzith.bookstack.ui.theme.BookStackTheme

/**
 * BookStack Android App - MainActivity (2026-01-05)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keystoreManager = BookStackApplication.instance.keystoreManager
        val startDestination = if (keystoreManager.isConfigured()) {
            Screen.Library.route
        } else {
            Screen.Settings.route
        }

        setContent {
            BookStackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    BookStackNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
