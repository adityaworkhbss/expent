package com.aditya.expent.presentation.onboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aditya.expent.presentation.dashboard.DashboardActivity
import com.aditya.expent.presentation.theme.ExpentTheme
import com.aditya.expent.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    private val viewModel: OnboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ExpentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardRoute(
                        viewModel = viewModel,
                        onFinish = {
                            sessionManager.setOnboardingComplete(true)
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}