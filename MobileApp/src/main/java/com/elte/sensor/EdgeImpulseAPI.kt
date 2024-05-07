package com.elte.sensor

import java.io.File
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Interface for Edge Impulse API.
 * @author Wittawin Panta
 * @version 1.0 2024-04-13
 */
interface EdgeImpulseAPI {
    @POST("api/training/data")
    fun uploadFile(
        @Header("x-api-key") apiKey: String,
        @Body file: File
    ): Call<Void>
}
