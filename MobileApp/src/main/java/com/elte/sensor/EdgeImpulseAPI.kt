package com.elte.sensor

import java.io.File
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Interface for Edge Impulse API.
 * @author Wittawin Panta
 * @version 2.0 2024-05-26
 */
interface EdgeImpulseAPI {
    @POST("api/training/data")
    fun uploadFile(
        @Header("x-api-key") apiKey: String,
        @Body file: File
    ): Call<Void>
}
