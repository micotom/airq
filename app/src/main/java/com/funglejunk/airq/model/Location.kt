package com.funglejunk.airq.model

data class Location(val latitude: Double, val longitude: Double) {

    companion object {
        val Invalid = Location(-1.0, -1.0)
        private val degToRad = Math.PI / 180.0
        val earthRadius = 6371000.0 // in meters
    }

    val isValid = this !== Invalid

    // Taken from https://software.intel.com/node/341473
    fun distanceTo(otherLocation: Location): Double {
        val phi1 = latitude * degToRad
        val phi2 = otherLocation.latitude * degToRad
        val lam1 = longitude * degToRad
        val lam2 = otherLocation.longitude * degToRad
        return earthRadius * Math.acos(
                Math.sin(phi1) * Math.sin(phi2) + Math.cos(phi1) * Math.cos(phi2) * Math.cos(lam2 - lam1)
        )
    }

}