package com.globallogic.thespaceapp.data.remote.response.rockets


import com.google.gson.annotations.SerializedName

data class Mass(
    @SerializedName("kg")
    val kg: Int,
    @SerializedName("lb")
    val lb: Int
)