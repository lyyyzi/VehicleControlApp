package com.example.vehiclecontrolapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Android 页面所需要的全部状态。
 */
data class VehicleUiState(
    val isLocked: Boolean = true,
    val isOnline: Boolean? = null,
    val isInitializing: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 管理车辆页面状态和用户操作。
 */
class VehicleViewModel(
    private val repository: VehicleRepository =
        VehicleRepository()
) : ViewModel() {

    var uiState by mutableStateOf(
        VehicleUiState()
    )
        private set

    init {
        loadInitialStatus()
    }

    /**
     * App 第一次启动时读取后端车辆状态。
     */
    private fun loadInitialStatus() {
        viewModelScope.launch {
            uiState = uiState.copy(
                isInitializing = true,
                errorMessage = null
            )

            try {
                val vehicle =
                    repository.getVehicleStatus()

                uiState = uiState.copy(
                    isLocked = vehicle.locked,
                    isOnline = vehicle.online,
                    errorMessage =
                    if (vehicle.online) {
                        null
                    } else {
                        "Vehicle is offline"
                    }
                )
            } catch (exception: Exception) {
                uiState = uiState.copy(
                    errorMessage =
                    exception.message
                        ?: "Failed to load vehicle status"
                )
            } finally {
                uiState = uiState.copy(
                    isInitializing = false
                )
            }
        }
    }

    /**
     * 用户点击 Refresh Status 时执行。
     */
    fun refreshStatus() {
        if (uiState.isLoading) {
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true
            )

            try {
                val vehicle =
                    repository.getVehicleStatus()

                uiState = uiState.copy(
                    isLocked = vehicle.locked,
                    isOnline = vehicle.online,
                    errorMessage =
                    if (vehicle.online) {
                        null
                    } else {
                        "Vehicle is offline"
                    }
                )
            } catch (exception: Exception) {
                uiState = uiState.copy(
                    errorMessage =
                    exception.message
                        ?: "Failed to refresh vehicle status"
                )
            } finally {
                uiState = uiState.copy(
                    isLoading = false
                )
            }
        }
    }

    /**
     * 根据当前车锁状态发送 LOCK 或 UNLOCK。
     */
    fun sendVehicleCommand() {
        val currentState = uiState

        if (
            currentState.isInitializing ||
            currentState.isLoading ||
            currentState.isOnline != true
        ) {
            return
        }

        val commandType =
            if (currentState.isLocked) {
                "UNLOCK"
            } else {
                "LOCK"
            }

        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                when (
                    val result =
                        repository.sendCommand(
                            commandType
                        )
                ) {
                    is VehicleCommandResult.Success -> {
                        uiState = uiState.copy(
                            isLocked =
                            result.vehicle.locked,
                            isOnline =
                            result.vehicle.online,
                            errorMessage = null
                        )
                    }

                    is VehicleCommandResult.Failed -> {
                        val latestVehicle =
                            result.vehicle

                        uiState = uiState.copy(
                            isLocked =
                            latestVehicle?.locked
                                ?: uiState.isLocked,
                            isOnline =
                            latestVehicle?.online
                                ?: uiState.isOnline,
                            errorMessage =
                            result.message
                        )
                    }

                    is VehicleCommandResult.TimedOut -> {
                        val latestVehicle =
                            result.vehicle

                        uiState = uiState.copy(
                            isLocked =
                            latestVehicle?.locked
                                ?: uiState.isLocked,
                            isOnline =
                            latestVehicle?.online
                                ?: uiState.isOnline,
                            errorMessage =
                            "Unable to confirm vehicle " +
                                    "response. Latest status " +
                                    "was refreshed."
                        )
                    }
                }
            } catch (exception: Exception) {
                uiState = uiState.copy(
                    errorMessage =
                    exception.message
                        ?: "Unknown network error"
                )
            } finally {
                uiState = uiState.copy(
                    isLoading = false
                )
            }
        }
    }
}