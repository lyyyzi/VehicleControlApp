package com.example.vehiclecontrolapp

import kotlinx.coroutines.delay

sealed class VehicleCommandResult {

    data class Success(
        val commandId: String,
        val vehicle: VehicleState
    ) : VehicleCommandResult()

    data class Failed(
        val commandId: String,
        val message: String,
        val vehicle: VehicleState?
    ) : VehicleCommandResult()

    data class TimedOut(
        val commandId: String,
        val vehicle: VehicleState?
    ) : VehicleCommandResult()
}

class VehicleRepository(
    private val api: VehicleApi = ApiClient.vehicleApi,
    private val pollingIntervalMs: Long = 500L,
    private val timeoutMs: Long = 10_000L
) {

    suspend fun getVehicleStatus(): VehicleState {
        return api.getVehicleStatus()
    }

    suspend fun sendCommand(
        commandType: String
    ): VehicleCommandResult {

        // 第一步：向后端发送 LOCK 或 UNLOCK
        val acceptedResponse =
            api.sendCommand(
                CommandRequest(
                    commandType = commandType
                )
            )

        val commandId = acceptedResponse.commandId

        // 10 秒 ÷ 500 毫秒 = 最多查询 20 次
        val maximumAttempts =
            (timeoutMs / pollingIntervalMs)
                .toInt()
                .coerceAtLeast(1)

        repeat(maximumAttempts) {
            delay(pollingIntervalMs)

            val commandStatus =
                api.getCommandStatus(commandId)

            when (commandStatus.status.uppercase()) {

                "ACCEPTED",
                "PROCESSING" -> {
                    // 命令仍在处理中，继续下一次查询
                }

                "COMPLETED" -> {
                    val vehicle =
                        commandStatus.vehicle
                            ?: api.getVehicleStatus()

                    return VehicleCommandResult.Success(
                        commandId = commandId,
                        vehicle = vehicle
                    )
                }

                "FAILED" -> {
                    return VehicleCommandResult.Failed(
                        commandId = commandId,
                        message =
                        commandStatus.message
                            ?: "Vehicle command failed",
                        vehicle = getLatestVehicleOrNull()
                    )
                }

                else -> {
                    return VehicleCommandResult.Failed(
                        commandId = commandId,
                        message =
                        "Unknown command status: " +
                                commandStatus.status,
                        vehicle = getLatestVehicleOrNull()
                    )
                }
            }
        }

        // 查询达到最大次数，仍然没有最终结果
        return VehicleCommandResult.TimedOut(
            commandId = commandId,
            vehicle = getLatestVehicleOrNull()
        )
    }

    private suspend fun getLatestVehicleOrNull():
            VehicleState? {

        return try {
            api.getVehicleStatus()
        } catch (exception: Exception) {
            null
        }
    }
}