package com.example.vehiclecontrolapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VehicleApi {

    @GET("vehicles/vehicle-001")
    suspend fun getVehicleStatus(): VehicleState

    @POST("vehicles/vehicle-001/commands")
    suspend fun sendCommand(
        @Body request: CommandRequest
    ): CommandAcceptedResponse

    @GET("commands/{commandId}")
    suspend fun getCommandStatus(
        @Path("commandId") commandId: String
    ): CommandStatusResponse
}

object ApiClient {

    val vehicleApi: VehicleApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VehicleApi::class.java)
    }
}