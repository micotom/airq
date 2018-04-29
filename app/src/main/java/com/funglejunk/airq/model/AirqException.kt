package com.funglejunk.airq.model

sealed class AirqException(msg: String) : RuntimeException(msg) {

    class NoUserLocationException : AirqException("Cannot determine user location")

    class AirInfoParserException : AirqException("Could not parse Luft Jetzt api result")

}