package com.example.vehiclecontrolapp

import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

class BackendResetRule : ExternalResource() {

    override fun before() {
        val response =
            runBlocking {
                TestControlApiClient.api
                    .resetSimulator()
            }

        check(response.isSuccessful) {
            "Failed to reset backend. " +
                    "Is FastAPI running?"
        }
    }
}