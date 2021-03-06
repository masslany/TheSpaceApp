package com.masslany.thespaceapp.domain.model

import com.masslany.thespaceapp.data.remote.response.dragons.HeatShield
import com.masslany.thespaceapp.data.remote.response.dragons.PayloadInfo
import com.masslany.thespaceapp.data.remote.response.dragons.Thruster

data class DragonModel(
    val name: String,
    val active: Boolean,
    val crewCapacity: Int,
    val description: String,
    val diameter: Double,
    val dryMass: Int,
    val firstFlight: String,
    val flickrImages: List<String>,
    val id: String,
    val wikipedia: String,
    val heightWTrunk: Double,
    val payloadInfo: PayloadInfo,
    val heatShield: HeatShield,
    val thrusters: List<Thruster>,
)