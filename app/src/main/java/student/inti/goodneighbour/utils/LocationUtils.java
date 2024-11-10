package student.inti.goodneighbour.utils;

import java.util.Arrays;
import java.util.List;

public class LocationUtils {
    // List of Penang locations
    public static final List<String> PENANG_LOCATIONS = Arrays.asList(
            "George Town",
            "Bayan Lepas",
            "Butterworth",
            "Bukit Mertajam",
            "Nibong Tebal",
            "Kepala Batas",
            "Tanjung Bungah",
            "Batu Ferringhi",
            "Air Itam",
            "Balik Pulau",
            "Teluk Bahang",
            "Pulau Tikus",
            "Jelutong",
            "Gelugor",
            "Sungai Ara",
            "Permatang Pauh"
    );

    // Method to get sorted locations
    public static List<String> getLocations() {
        return PENANG_LOCATIONS;
    }

    // Method to validate if a location is valid
    public static boolean isValidLocation(String location) {
        return location != null && PENANG_LOCATIONS.contains(location);
    }
}