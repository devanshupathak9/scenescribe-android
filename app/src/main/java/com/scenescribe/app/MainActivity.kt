package com.scenescribe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.scenescribe.app.data.TokenManager
import com.scenescribe.app.navigation.SceneScribeNavGraph
import com.scenescribe.app.ui.theme.SceneScribeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        val initialUser = tokenManager.getUser()

        setContent {
            SceneScribeTheme {
                SceneScribeNavGraph(
                    tokenManager = tokenManager,
                    initialUser  = initialUser
                )
            }
        }
    }
}
