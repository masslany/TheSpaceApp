package com.masslany.thespaceapp.data.remote.response.rockets


import com.google.gson.annotations.SerializedName

data class ThrustVacuum(
    @SerializedName("kN")
    val kN: Double,
    @SerializedName("lbf")
    val lbf: Double
)