package com.example.backend.util;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageHashGenerator {

    /**
     * Generates a 64-bit perceptual Average Hash (aHash) for an uploaded image.
     * 
     * @param imageFile The MultipartFile image.
     * @return A 64-character binary string representing the image fingerprint, or null on error.
     */
    public static String generatePerceptualHash(MultipartFile imageFile) {
        try {
            BufferedImage original = ImageIO.read(imageFile.getInputStream());
            if (original == null) {
                return null;
            }

            // 1. Resize to 8x8 pixels
            BufferedImage resized = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = resized.createGraphics();
            g.drawImage(original, 0, 0, 8, 8, null);
            g.dispose();

            // 2. Compute average intensity
            double sum = 0;
            int[] pixels = new int[64];
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int gray = resized.getRaster().getSample(x, y, 0);
                    pixels[y * 8 + x] = gray;
                    sum += gray;
                }
            }
            double average = sum / 64.0;

            // 3. Build hash string (1 if >= average, 0 otherwise)
            StringBuilder hashBuilder = new StringBuilder();
            for (int pixel : pixels) {
                hashBuilder.append(pixel >= average ? "1" : "0");
            }

            return hashBuilder.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to read image for perceptual hashing analysis", e);
        }
    }

    /**
     * Calculates the Hamming Distance (similarity score) between two perceptual hashes.
     * 
     * @param hash1 64-bit binary string.
     * @param hash2 64-bit binary string.
     * @return The number of differing bits. A distance of 0 means identical images.
     */
    public static int calculateHammingDistance(String hash1, String hash2) {
        if (hash1 == null || hash2 == null || hash1.length() != hash2.length()) {
            return 999;
        }

        int distance = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }
}
