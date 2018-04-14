package com.funglejunk.airq.logic.location

data class Location(val latitude: Double, val longitude: Double) {

    companion object {
        val Invalid = Location(-1.0, -1.0)
    }

    val isValid = this !== Invalid

}