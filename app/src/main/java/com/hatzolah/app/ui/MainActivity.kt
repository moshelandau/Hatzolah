package com.hatzolah.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.hatzolah.app.ui.auth.AuthScreen
import com.hatzolah.app.ui.navigation.AppNavigation
import com.hatzolah.app.ui.theme.HatzolahTheme
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HatzolahTheme {
                var isLoggedIn by remember { mutableStateOf(preferencesManager.isLoggedIn()) }

                if (isLoggedIn) {
                    AppNavigation()
                } else {
                    AuthScreen(
                        onAuthSuccess = { isLoggedIn = true }
                    )
                }
            }
        }
    }
}
