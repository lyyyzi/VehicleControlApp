package com.example.vehiclecontrolapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val vehicleViewModel:
                            VehicleViewModel = viewModel()

                    VehicleScreen(
                        uiState =
                        vehicleViewModel.uiState,

                        onVehicleCommandClick = {
                            vehicleViewModel
                                .sendVehicleCommand()
                        },

                        onRefreshClick = {
                            vehicleViewModel
                                .refreshStatus()
                        }
                    )
                }
            }
        }
    }
}