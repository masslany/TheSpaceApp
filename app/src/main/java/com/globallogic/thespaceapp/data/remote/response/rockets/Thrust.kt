package com.globallogic.thespaceapp.data.remote.response.rockets


import com.google.gson.annotations.SerializedName

data class Thrust(
    @SerializedName("kN")
    val kN: Double,
    @SerializedName("lbf")
    val lbf: Double
)