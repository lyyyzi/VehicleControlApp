package com.example.vehiclecontrolapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VehicleScreen(
    uiState: VehicleUiState,
    onVehicleCommandClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = when {
                uiState.isInitializing ->
                    "Loading vehicle status..."

                uiState.isLoading ->
                    "Sending command..."

                uiState.isLocked ->
                    "Vehicle Status: Locked"

                else ->
                    "Vehicle Status: Unlocked"
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = when (uiState.isOnline) {
                true -> "Connection: Online"
                false -> "Connection: Offline"
                null -> "Connection: Unknown"
            }
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = "Error: $message"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        Button(
            enabled =
            !uiState.isInitializing &&
                    !uiState.isLoading &&
                    uiState.isOnline == true,

            onClick = onVehicleCommandClick
        ) {
            Text(
                text =
                if (uiState.isLocked) {
                    "Unlock Vehicle"
                } else {
                    "Lock Vehicle"
                }
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // Refresh 现在始终显示
        Button(
            enabled =
            !uiState.isLoading &&
                    !uiState.isInitializing,

            onClick = onRefreshClick
        ) {
            Text("Refresh Status")
        }

        if (
            uiState.isInitializing ||
            uiState.isLoading
        ) {
            Spacer(
                modifier = Modifier.height(20.dp)
            )

            CircularProgressIndicator()
        }
    }
}