package com.funglejunk.airq.model

sealed class AirqException(msg: String) : RuntimeException(msg) {

    class NoUserLocation : AirqException("Cannot determine user location") // TODO check for usage in main stream

    class AirInfoParser : AirqException("Could not parse 'Luft Jetzt' api result")

    class OpenAqParser : AirqException("Could not parse 'Open AQ' api result")

    class NoLocationPermission : AirqException("No location permission given")

    class NoNetwork : AirqException("No network connection available")

    class InvalidLastKnownLocation : AirqException("Invalid last known location")

    class NoStandardizedMeasurement : AirqException("Cannot standardize measurement")

    class NoAirInfoMeasurement : AirqException("No measurements in api object")

    class NoOpenAqDateMeasurement : AirqException("No latest date in open aq object")

}