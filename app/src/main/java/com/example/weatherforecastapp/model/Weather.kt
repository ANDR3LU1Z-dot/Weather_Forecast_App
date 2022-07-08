package com.example.weatherforecastapp.model

import com.google.gson.annotations.SerializedName
import java.net.IDN

data class Weather(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String

)
