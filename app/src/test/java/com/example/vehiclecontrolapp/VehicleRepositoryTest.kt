package com.example.vehiclecontrolapp

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

class VehicleRepositoryTest {

    /**
     * 假的 VehicleApi。
     *
     * 它不会访问真实 FastAPI，而是返回测试预先设置的数据。
     */
    private class FakeVehicleApi : VehicleApi {

        var vehicleState = VehicleState(
            vehicleId = "vehicle-001",
            locked = true,
            online = true
        )

        private val commandStatuses =
            ArrayDeque<CommandStatusResponse>()

        fun addCommandStatus(
            status: CommandStatusResponse
        ) {
            commandStatuses.addLast(status)
        }

        override suspend fun getVehicleStatus():
                VehicleState {

            return vehicleState
        }

        override suspend fun sendCommand(
            request: CommandRequest
        ): CommandAcceptedResponse {

            return CommandAcceptedResponse(
                commandId = "cmd-test-001",
                commandType = request.commandType,
                status = "ACCEPTED"
            )
        }

        override suspend fun getCommandStatus(
            commandId: String
        ): CommandStatusResponse {

            if (commandStatuses.isEmpty()) {
                return CommandStatusResponse(
                    commandId = commandId,
                    commandType = "UNLOCK",
                    status = "PROCESSING"
                )
            }

            return commandStatuses.removeFirst()
        }
    }

    @Test
    fun sendCommand_returnsSuccess_whenCommandCompletes() =
        runTest {

            val fakeApi = FakeVehicleApi()

            fakeApi.addCommandStatus(
                CommandStatusResponse(
                    commandId = "cmd-test-001",
                    commandType = "UNLOCK",
                    status = "PROCESSING"
                )
            )

            fakeApi.addCommandStatus(
                CommandStatusResponse(
                    commandId = "cmd-test-001",
                    commandType = "UNLOCK",
                    status = "COMPLETED",
                    vehicle = VehicleState(
                        vehicleId = "vehicle-001",
                        locked = false,
                        online = true
                    )
                )
            )

            val repository = VehicleRepository(
                api = fakeApi,
                pollingIntervalMs = 100L,
                timeoutMs = 1_000L
            )

            val result =
                repository.sendCommand("UNLOCK")

            assertTrue(
                result is VehicleCommandResult.Success
            )

            val success =
                result as VehicleCommandResult.Success

            assertEquals(
                "cmd-test-001",
                success.commandId
            )

            assertFalse(
                success.vehicle.locked
            )

            assertTrue(
                success.vehicle.online
            )
        }

    @Test
    fun sendCommand_returnsFailed_whenVehicleIsOffline() =
        runTest {

            val fakeApi = FakeVehicleApi()

            fakeApi.vehicleState =
                VehicleState(
                    vehicleId = "vehicle-001",
                    locked = true,
                    online = false
                )

            fakeApi.addCommandStatus(
                CommandStatusResponse(
                    commandId = "cmd-test-001",
                    commandType = "UNLOCK",
                    status = "FAILED",
                    message = "Vehicle is offline"
                )
            )

            val repository = VehicleRepository(
                api = fakeApi,
                pollingIntervalMs = 100L,
                timeoutMs = 1_000L
            )

            val result =
                repository.sendCommand("UNLOCK")

            assertTrue(
                result is VehicleCommandResult.Failed
            )

            val failure =
                result as VehicleCommandResult.Failed

            assertEquals(
                "Vehicle is offline",
                failure.message
            )

            assertEquals(
                false,
                failure.vehicle?.online
            )

            assertEquals(
                true,
                failure.vehicle?.locked
            )
        }

    @Test
    fun sendCommand_returnsTimedOut_whenStatusNeverFinishes() =
        runTest {

            val fakeApi = FakeVehicleApi()

            // Fake API 没有加入最终状态，
            // 所以每次查询都会返回 PROCESSING。

            val repository = VehicleRepository(
                api = fakeApi,
                pollingIntervalMs = 100L,
                timeoutMs = 300L
            )

            val result =
                repository.sendCommand("UNLOCK")

            assertTrue(
                result is VehicleCommandResult.TimedOut
            )

            val timedOut =
                result as VehicleCommandResult.TimedOut

            assertEquals(
                "cmd-test-001",
                timedOut.commandId
            )

            // 命令没有完成，所以车辆仍然锁定。
            assertEquals(
                true,
                timedOut.vehicle?.locked
            )
        }
}