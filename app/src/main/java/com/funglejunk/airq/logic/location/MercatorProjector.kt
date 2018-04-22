package com.funglejunk.airq.logic.location

import com.funglejunk.airq.model.Location


// from: https://github.com/mapsforge/mapsforge/blob/master/mapsforge-core/src/main/java/org/mapsforge/core/util/MercatorProjection.java
object MercatorProjector {

    data class MercatorPoint(var x: Double, var y: Double)

    /**
     * @param scaleFactor the scale factor for which the size of the world map should be returned.
     * @return the horizontal and vertical size of the map in pixel at the given scale.
     * @throws IllegalArgumentException if the given scale factor is < 1
     */
    private fun getMapSizeWithScaleFactor(scaleFactor: Double, tileSize: Int): Long {
        if (scaleFactor < 1) {
            throw IllegalArgumentException("scale factor must not < 1 " + scaleFactor)
        }
        return (tileSize * Math.pow(2.0, scaleFactorToZoomLevel(scaleFactor))).toLong()
    }

    fun getPixelWithScaleFactor(latLong: Location, scaleFactor: Double, tileSize: Int): MercatorPoint {
        val pixelX = longitudeToPixelXWithScaleFactor(latLong.longitude, scaleFactor, tileSize)
        val pixelY = latitudeToPixelYWithScaleFactor(latLong.latitude, scaleFactor, tileSize)
        return MercatorPoint(pixelX, pixelY)
    }

    /**
     * Converts a latitude coordinate (in degrees) to a pixel Y coordinate at a certain scale.
     *
     * @param latitude    the latitude coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the pixel Y coordinate of the latitude value.
     */
    private fun latitudeToPixelYWithScaleFactor(latitude: Double, scaleFactor: Double, tileSize: Int): Double {
        val sinLatitude = Math.sin(latitude * (Math.PI / 180))
        val mapSize = getMapSizeWithScaleFactor(scaleFactor, tileSize)
        // FIXME improve this formula so that it works correctly without the clipping
        val pixelY = (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI)) * mapSize
        return Math.min(Math.max(0.0, pixelY), mapSize.toDouble())
    }

    /**
     * Converts a longitude coordinate (in degrees) to a pixel X coordinate at a certain scale factor.
     *
     * @param longitude   the longitude coordinate that should be converted.
     * @param scaleFactor the scale factor at which the coordinate should be converted.
     * @return the pixel X coordinate of the longitude value.
     */
    private fun longitudeToPixelXWithScaleFactor(longitude: Double, scaleFactor: Double, tileSize: Int): Double {
        val mapSize = getMapSizeWithScaleFactor(scaleFactor, tileSize)
        return (longitude + 180) / 360 * mapSize
    }

    /**
     * Converts a scaleFactor to a zoomLevel.
     * Note that this will return a double, as the scale factors cover the
     * intermediate zoom levels as well.
     *
     * @param scaleFactor the scale factor to convert to a zoom level.
     * @return the zoom level.
     */
    private fun scaleFactorToZoomLevel(scaleFactor: Double): Double {
        return Math.log(scaleFactor) / Math.log(2.0)
    }

}