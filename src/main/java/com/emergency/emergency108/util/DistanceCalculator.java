package com.emergency.emergency108.util;

/**
 * Utility class for geographic distance calculations.
 * Uses Haversine formula to calculate distance between two points on Earth.
 */
public class DistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000; // Earth's radius in meters

    /**
     * Calculate distance between two geographic coordinates using Haversine formula.
     * 
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in meters
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance in meters
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Check if two points are within a specified distance threshold.
     * 
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @param thresholdMeters Maximum distance in meters
     * @return true if points are within threshold, false otherwise
     */
    public static boolean isWithinDistance(double lat1, double lon1, double lat2, double lon2, double thresholdMeters) {
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance <= thresholdMeters;
    }

    /**
     * Check if a point is within 100 meters of target point.
     * Used for hospital arrival validation.
     * 
     * @param currentLat Current latitude in degrees
     * @param currentLon Current longitude in degrees
     * @param targetLat Target latitude in degrees
     * @param targetLon Target longitude in degrees
     * @return true if within 100 meters, false otherwise
     */
    public static boolean isWithin100Meters(double currentLat, double currentLon, double targetLat, double targetLon) {
        return isWithinDistance(currentLat, currentLon, targetLat, targetLon, 100.0);
    }
}
