package com.example.panel_control_peso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.panel_control_peso.ui.screens.AgroGreenDark
import com.example.panel_control_peso.ui.screens.DashboardScreen
import com.example.panel_control_peso.ui.theme.Panel_control_pesoTheme
import com.example.panel_control_peso.ui.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Panel_control_pesoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AgroGreenDark
                ) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}