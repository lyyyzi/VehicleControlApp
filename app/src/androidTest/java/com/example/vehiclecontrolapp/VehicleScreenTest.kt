package com.example.vehiclecontrolapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VehicleScreenTest {

    @get:Rule
    val composeTestRule =
        createComposeRule()

    @Test
    fun onlineLockedVehicle_showsUnlockButton() {
        var commandClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                VehicleScreen(
                    uiState = VehicleUiState(
                        isLocked = true,
                        isOnline = true,
                        isInitializing = false,
                        isLoading = false
                    ),
                    onVehicleCommandClick = {
                        commandClicked = true
                    },
                    onRefreshClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                "Vehicle Status: Locked"
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "Connection: Online"
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "Unlock Vehicle"
            )
            .assertIsEnabled()
            .performClick()

        assertTrue(commandClicked)
    }

    @Test
    fun unlockedVehicle_showsLockButton() {
        composeTestRule.setContent {
            MaterialTheme {
                VehicleScreen(
                    uiState = VehicleUiState(
                        isLocked = false,
                        isOnline = true,
                        isInitializing = false,
                        isLoading = false
                    ),
                    onVehicleCommandClick = {},
                    onRefreshClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                "Vehicle Status: Unlocked"
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "Lock Vehicle"
            )
            .assertIsEnabled()
    }

    @Test
    fun offlineVehicle_disablesCommandAndShowsRefresh() {
        var refreshClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                VehicleScreen(
                    uiState = VehicleUiState(
                        isLocked = true,
                        isOnline = false,
                        isInitializing = false,
                        isLoading = false,
                        errorMessage =
                        "Vehicle is offline"
                    ),
                    onVehicleCommandClick = {},
                    onRefreshClick = {
                        refreshClicked = true
                    }
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                "Connection: Offline"
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "Error: Vehicle is offline"
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "Unlock Vehicle"
            )
            .assertIsNotEnabled()

        composeTestRule
            .onNodeWithText(
                "Refresh Status"
            )
            .assertIsEnabled()
            .performClick()

        assertTrue(refreshClicked)
    }

    @Test
    fun loadingState_disablesVehicleButton() {
        composeTestRule.setContent {
            MaterialTheme {
                VehicleScreen(
                    uiState = VehicleUiState(
                        isLocked = true,
                        isOnline = true,
                        isInitializing = false,
                        isLoading = true
                    ),
                    onVehicleCommandClick = {},
                    onRefreshClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                "Sending command..."
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "Unlock Vehicle"
            )
            .assertIsNotEnabled()
    }
}