package com.example.vehiclecontrolapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.assertIsNotEnabled

class VehicleE2ETest {

    private val backendResetRule =
        BackendResetRule()

    private val composeRule =
        createAndroidComposeRule<MainActivity>()

    private fun setVehicleOnline(
        online: Boolean
    ) {
        val response =
            runBlocking {
                TestControlApiClient.api
                    .setConnectivity(
                        TestConnectivityRequest(
                            online = online
                        )
                    )
            }

        check(response.isSuccessful)
    }

    private fun setFailureMode(
        mode: String
    ) {
        val response =
            runBlocking {
                TestControlApiClient.api
                    .setFailureMode(
                        TestFailureModeRequest(
                            failure_mode = mode
                        )
                    )
            }

        check(response.isSuccessful)
    }

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain
            .outerRule(backendResetRule)
            .around(composeRule)

    @Test
    fun unlockVehicle_endToEnd_updatesUi() {

        // 等待 App 从真实 FastAPI 读取初始状态
        composeRule.waitUntil(
            timeoutMillis = 5_000
        ) {
            composeRule
                .onAllNodesWithText(
                    "Connection: Online"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // 初始状态必须是 Locked
        composeRule
            .onNodeWithText(
                "Vehicle Status: Locked"
            )
            .assertIsDisplayed()

        // Unlock 按钮应该可点击
        composeRule
            .onNodeWithText(
                "Unlock Vehicle"
            )
            .assertIsEnabled()
            .performClick()

        // 后端正在处理命令
        composeRule
            .onNodeWithText(
                "Sending command..."
            )
            .assertIsDisplayed()

        // 等待真实后端完成命令
        composeRule.waitUntil(
            timeoutMillis = 15_000
        ) {
            composeRule
                .onAllNodesWithText(
                    "Vehicle Status: Unlocked"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // 最终 UI 必须显示 Unlocked
        composeRule
            .onNodeWithText(
                "Vehicle Status: Unlocked"
            )
            .assertIsDisplayed()

        // 按钮应该变成 Lock Vehicle
        composeRule
            .onNodeWithText(
                "Lock Vehicle"
            )
            .assertIsDisplayed()
    }
    @Test
    fun unlockThenLock_endToEnd_updatesUi() {

        composeRule.waitUntil(
            timeoutMillis = 5_000
        ) {
            composeRule
                .onAllNodesWithText(
                    "Vehicle Status: Locked"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Unlock
        composeRule
            .onNodeWithText("Unlock Vehicle")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 15_000
        ) {
            composeRule
                .onAllNodesWithText(
                    "Vehicle Status: Unlocked"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Lock
        composeRule
            .onNodeWithText("Lock Vehicle")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 15_000
        ) {
            composeRule
                .onAllNodesWithText(
                    "Vehicle Status: Locked"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText("Unlock Vehicle")
            .assertIsDisplayed()
    }
    @Test
    fun vehicleOffline_endToEnd_showsOfflineState() {

        // 后端把模拟车辆设置成 offline
        setVehicleOnline(false)

        // App 重新读取后端状态
        composeRule
            .onNodeWithText("Refresh Status")
            .performClick()

        composeRule.waitUntil(
            timeoutMillis = 5_000
        ) {
            composeRule
                .onAllNodesWithText(
                    "Connection: Offline"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Connection: Offline"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Error: Vehicle is offline"
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(
                "Unlock Vehicle"
            )
            .assertIsNotEnabled()
    }
    @Test
    fun commandTimeout_endToEnd_showsTimeoutError() {

        setVehicleOnline(true)
        setFailureMode("timeout")

        composeRule.waitUntil(
            timeoutMillis = 5_000
        ) {
            composeRule
                .onAllNodesWithText(
                    "Unlock Vehicle"
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Unlock Vehicle"
            )
            .performClick()

        // Repository 自己会等待约 10 秒
        composeRule.waitUntil(
            timeoutMillis = 15_000
        ) {
            composeRule
                .onAllNodesWithText(
                    "Error: Unable to confirm vehicle response. " +
                            "Latest status was refreshed."
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithText(
                "Error: Unable to confirm vehicle response. " +
                        "Latest status was refreshed."
            )
            .assertIsDisplayed()

        // Timeout 不代表车真的解锁了
        composeRule
            .onNodeWithText(
                "Vehicle Status: Locked"
            )
            .assertIsDisplayed()
    }
}