package com.example.vehiclecontrolapp

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST

data class TestConnectivityRequest(
    val online: Boolean
)

data class TestFailureModeRequest(
    val failure_mode: String
)

interface TestControlApi {

    @POST("simulator/reset")
    suspend fun resetSimulator():
            Response<ResponseBody>

    @PATCH("vehicles/vehicle-001/connectivity")
    suspend fun setConnectivity(
        @Body request: TestConnectivityRequest
    ): Response<ResponseBody>

    @PATCH("simulator/failure-mode")
    suspend fun setFailureMode(
        @Body request: TestFailureModeRequest
    ): Response<ResponseBody>
}

object TestControlApiClient {

    val api: TestControlApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(TestControlApi::class.java)
    }
}