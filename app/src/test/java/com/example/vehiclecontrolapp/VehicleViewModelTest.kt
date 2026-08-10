package com.example.vehiclecontrolapp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.ArrayDeque

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModelTest {

    @get:Rule
    val mainDispatcherRule =
        MainDispatcherRule()

    /**
     * 假的 API。
     *
     * 测试不会连接真实 FastAPI，
     * 所有返回结果都由测试代码控制。
     */
    private class FakeVehicleApi : VehicleApi {

        var vehicleState =
            VehicleState(
                vehicleId = "vehicle-001",
                locked = true,
                online = true
            )

        var lastCommandType: String? = null

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

            lastCommandType =
                request.commandType

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
                    commandType =
                    lastCommandType ?: "UNLOCK",
                    status = "PROCESSING"
                )
            }

            return commandStatuses.removeFirst()
        }
    }

    private fun createRepository(
        fakeApi: FakeVehicleApi,
        timeoutMs: Long = 1_000L
    ): VehicleRepository {

        return VehicleRepository(
            api = fakeApi,
            pollingIntervalMs = 100L,
            timeoutMs = timeoutMs
        )
    }

    @Test
    fun initialState_loadsVehicleStatus() =
        runTest {

            val fakeApi =
                FakeVehicleApi()

            fakeApi.vehicleState =
                VehicleState(
                    vehicleId = "vehicle-001",
                    locked = false,
                    online = true
                )

            val viewModel =
                VehicleViewModel(
                    repository =
                    createRepository(fakeApi)
                )

            // 执行 ViewModel init 中启动的协程
            advanceUntilIdle()

            val state =
                viewModel.uiState

            assertFalse(state.isInitializing)
            assertFalse(state.isLocked)
            assertEquals(true, state.isOnline)
            assertNull(state.errorMessage)
        }

    @Test
    fun sendVehicleCommand_updatesState_whenUnlockCompletes() =
        runTest {

            val fakeApi =
                FakeVehicleApi()

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
                    vehicle =
                    VehicleState(
                        vehicleId = "vehicle-001",
                        locked = false,
                        online = true
                    )
                )
            )

            val viewModel =
                VehicleViewModel(
                    repository =
                    createRepository(fakeApi)
                )

            // 完成初始状态读取
            advanceUntilIdle()

            viewModel.sendVehicleCommand()

            // 执行到 Repository 第一次 delay 为止
            runCurrent()

            assertTrue(
                viewModel.uiState.isLoading
            )

            // 自动推进虚拟时间，完成所有 delay
            advanceUntilIdle()

            val state =
                viewModel.uiState

            assertEquals(
                "UNLOCK",
                fakeApi.lastCommandType
            )

            assertFalse(state.isLoading)
            assertFalse(state.isLocked)
            assertEquals(true, state.isOnline)
            assertNull(state.errorMessage)
        }

    @Test
    fun sendVehicleCommand_showsError_whenVehicleIsOffline() =
        runTest {

            val fakeApi =
                FakeVehicleApi()

            fakeApi.addCommandStatus(
                CommandStatusResponse(
                    commandId = "cmd-test-001",
                    commandType = "UNLOCK",
                    status = "FAILED",
                    message = "Vehicle is offline"
                )
            )

            val viewModel =
                VehicleViewModel(
                    repository =
                    createRepository(fakeApi)
                )

            // 初始化时车辆在线，允许发送命令
            advanceUntilIdle()

            // 模拟命令发送后车辆掉线
            fakeApi.vehicleState =
                VehicleState(
                    vehicleId = "vehicle-001",
                    locked = true,
                    online = false
                )

            viewModel.sendVehicleCommand()

            advanceUntilIdle()

            val state =
                viewModel.uiState

            assertFalse(state.isLoading)
            assertTrue(state.isLocked)
            assertEquals(false, state.isOnline)

            assertEquals(
                "Vehicle is offline",
                state.errorMessage
            )
        }

    @Test
    fun sendVehicleCommand_showsTimeoutMessage_whenCommandNeverFinishes() =
        runTest {

            val fakeApi =
                FakeVehicleApi()

            // 不添加最终状态。
            // Fake API 每次都会返回 PROCESSING。

            val repository =
                createRepository(
                    fakeApi = fakeApi,
                    timeoutMs = 300L
                )

            val viewModel =
                VehicleViewModel(
                    repository = repository
                )

            advanceUntilIdle()

            viewModel.sendVehicleCommand()

            // 虚拟推进 300ms，不会真的等待
            advanceUntilIdle()

            val state =
                viewModel.uiState

            assertFalse(state.isLoading)
            assertTrue(state.isLocked)

            assertEquals(
                "Unable to confirm vehicle response. " +
                        "Latest status was refreshed.",
                state.errorMessage
            )
        }

    @Test
    fun refreshStatus_updatesLatestVehicleState() =
        runTest {

            val fakeApi =
                FakeVehicleApi()

            val viewModel =
                VehicleViewModel(
                    repository =
                    createRepository(fakeApi)
                )

            advanceUntilIdle()

            // 后端车辆状态发生变化
            fakeApi.vehicleState =
                VehicleState(
                    vehicleId = "vehicle-001",
                    locked = false,
                    online = true
                )

            viewModel.refreshStatus()

            advanceUntilIdle()

            val state =
                viewModel.uiState

            assertFalse(state.isLoading)
            assertFalse(state.isLocked)
            assertEquals(true, state.isOnline)
            assertNull(state.errorMessage)
        }
}