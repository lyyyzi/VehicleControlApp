package com.example.vehiclecontrolapp

import com.google.gson.annotations.SerializedName

data class CommandRequest(
    @SerializedName("command_type")
    val commandType: String
)

data class VehicleState(
    @SerializedName("vehicle_id")
    val vehicleId: String,

    val locked: Boolean,

    val online: Boolean
)

// POST 命令后第一次返回的数据
data class CommandAcceptedResponse(
    @SerializedName("command_id")
    val commandId: String,

    @SerializedName("command_type")
    val commandType: String,

    val status: String
)

// 查询命令状态时返回的数据
data class CommandStatusResponse(
    @SerializedName("command_id")
    val commandId: String,

    @SerializedName("command_type")
    val commandType: String,

    val status: String,

    val message: String? = null,

    // ACCEPTED 和 PROCESSING 时可能没有 vehicle
    val vehicle: VehicleState? = null
)