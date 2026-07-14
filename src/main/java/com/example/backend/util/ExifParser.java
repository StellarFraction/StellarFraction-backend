package com.example.backend.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public class ExifParser {

    /**
     * Extracts GPS coordinates (Latitude & Longitude) from an uploaded image file's EXIF metadata.
     * @param file The uploaded multipart image file.
     * @return A double array containing [Latitude, Longitude], or null if metadata is missing/incomplete.
     */
    public static double[] extractGpsCoordinates(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try (InputStream inputStream = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            
            if (gpsDirectory != null) {
                com.drew.lang.GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    return new double[]{geoLocation.getLatitude(), geoLocation.getLongitude()};
                }
            }
        } catch (Exception e) {
            // Log warning or silently handle files without EXIF records
            System.err.println("Warning: Failed to parse EXIF metadata: " + e.getMessage());
        }
        
        return null;
    }
}
