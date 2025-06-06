package com.xo.resumecrowded

import okhttp3.MultipartBody
import retrofit2.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("upload")
    suspend fun uploadPdf(
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>
}

val retrofit = Retrofit.Builder()
    .baseUrl("http://192.168.0.0:3000/")
    .addConverterFactory(ScalarsConverterFactory.create())
    .build()

val apiService = retrofit.create(ApiService::class.java)
