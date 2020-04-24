package ch.ethz.ikg.treasurehunt;

import android.location.Location;

/**
 * An utility class that contains functions that are used at various places.
 */
public class Util {
    /** The radius of our planet Earth. */
    public static final double EARTH_RADIUS = 6378137;

    /**
     * Computes the distance between two locations using the Haversine formula.
     *
     * @param origin The origin point.
     * @param destination The destination point.
     * @return The distance in meters.
     */
    public static double distance(Location origin, Location destination) {
        double a = origin.getLatitude() * Math.PI / 180.0;
        double b = destination.getLatitude() * Math.PI / 180.0;
        double deltaLat = a - b;

        double deltaLong = origin.getLongitude() * Math.PI / 180.0 - destination.getLongitude() * Math.PI / 180.0;
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(deltaLat / 2), 2) +
                Math.cos(a) * Math.cos(b) * Math.pow(Math.sin(deltaLong / 2), 2)));

        return s * EARTH_RADIUS;
    }
}
