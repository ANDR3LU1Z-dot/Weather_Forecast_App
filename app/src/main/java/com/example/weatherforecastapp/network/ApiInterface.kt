package com.example.weatherforecastapp.network

import retrofit2.Call
import com.example.weatherforecastapp.model.ModelMainData
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("weather")
    fun getCurrentWeatherData(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("APPID") api_key: String
    ): Call<ModelMainData>

    @GET("weather")
    fun getCityWeatherData(
        @Query("q") cityName: String,
        @Query("APPID") api_key: String
    ): Call<ModelMainData>

}