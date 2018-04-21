package com.funglejunk.airq.logic.location

data class Location(val latitude: Double, val longitude: Double) {

    companion object {
        val Invalid = Location(-1.0, -1.0)
    }

    val isValid = this !== Invalid

    // Taken from https://stackoverflow.com/a/837957/1785345
    fun distanceTo(otherLocation: Location): Double {
        val earthRadius = 6371000.0 //meters
        val dLat = Math.toRadians((otherLocation.latitude - latitude))
        val dLng = Math.toRadians((otherLocation.longitude - longitude))
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(otherLocation.latitude)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadius * c)
    }

}