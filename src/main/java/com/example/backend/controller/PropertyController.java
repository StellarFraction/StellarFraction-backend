package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.PortfolioResponse;
import com.example.backend.model.Property;
import com.example.backend.model.User;
import com.example.backend.service.PropertyService;
import com.example.backend.service.StorageService;
import com.example.backend.repository.PropertyRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;
import com.example.backend.util.ImageHashGenerator;
import com.example.backend.exception.ResourceNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final PropertyRepository propertyRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Property>> createListing(@RequestBody @org.springframework.lang.NonNull Property property) {
        java.util.Objects.requireNonNull(property.getSeller(), "Seller must not be null");
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        if (!authenticatedUser.getId().equals(property.getSeller().getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You cannot list a property on behalf of another user."));
        }
        if (!authenticatedUser.isIdentityVerified()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You must verify your identity via Dojah first to list properties."));
        }
        Property created = propertyService.createProperty(property, java.util.Objects.requireNonNull(property.getSeller().getId(), "Seller ID must not be null"));
        return ResponseEntity.ok(ApiResponse.success("Property created successfully", created));
    }

    @PostMapping(value = "/{id}/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Property>> uploadPropertyImage(
            @PathVariable("id") @org.springframework.lang.NonNull Long propertyId,
            @RequestParam("image") MultipartFile imageFile) {

        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (!property.getSeller().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not own this property listing."));
        }

        // 1. Generate Perceptual Average Hash (aHash) for the uploaded image
        String newHash = ImageHashGenerator.generatePerceptualHash(imageFile);
        
        Property matchedProperty = null;

        if (newHash != null) {
            // 2. Query all active listings with image hashes to check Hamming Distance
            List<Property> existingListings = propertyRepository.findByImageHashIsNotNull();
            for (Property existing : existingListings) {
                // Ignore matching images belonging to the same seller (e.g. updating photos)
                if (existing.getSeller().getId().equals(property.getSeller().getId())) {
                    continue;
                }
                
                int distance = ImageHashGenerator.calculateHammingDistance(newHash, existing.getImageHash());
                if (distance <= 5) { // Hamming threshold <= 5 indicates virtually identical images
                    matchedProperty = existing;
                    break;
                }
            }
        }

        // 3. Upload image securely to S3
        String uploadedUrl = storageService.uploadFile(imageFile, "properties/photos_" + propertyId);
        property.setImageUrl(uploadedUrl);
        property.setImageHash(newHash);

        if (matchedProperty != null) {
            property.setFlaggedAsDuplicate(true);
            property.setVerified(false); // Instantly de-verify due to duplicate flag
            propertyRepository.save(property);

            return ResponseEntity.badRequest().body(ApiResponse.error(
                String.format("Security Alert: This listing photograph matches an existing verified property listing (Property ID: %d) by a different host. Listing flagged and deactivated.", matchedProperty.getId())
            ));
        } else {
            property.setFlaggedAsDuplicate(false);
            propertyRepository.save(property);

            return ResponseEntity.ok(ApiResponse.success(
                "Property listing photograph uploaded and verified successfully! No duplicate footprints found.",
                property
            ));
        }
    }

    @GetMapping("/sale")
    public ResponseEntity<ApiResponse<List<Property>>> getPropertiesForSale() {
        return ResponseEntity.ok(ApiResponse.success("Fetched sale properties", propertyService.getPropertiesForSale()));
    }

    @GetMapping("/rent")
    public ResponseEntity<ApiResponse<List<Property>>> getPropertiesForRent() {
        return ResponseEntity.ok(ApiResponse.success("Fetched rent properties", propertyService.getPropertiesForRent()));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Property>>> getAllListings() {
        return ResponseEntity.ok(ApiResponse.success("Fetched all properties", propertyService.getAllProperties()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Property>> getPropertyById(@PathVariable("id") @org.springframework.lang.NonNull Long id) {
        Property property = propertyService.getPropertyById(id);
        return ResponseEntity.ok(ApiResponse.success("Property fetched successfully", property));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LANDLORD', 'AGENT')")
    public ResponseEntity<ApiResponse<Property>> updateProperty(
            @PathVariable("id") @org.springframework.lang.NonNull Long id,
            @RequestBody @org.springframework.lang.NonNull Property propertyDetails,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Property updated = propertyService.updateProperty(id, propertyDetails, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Property updated successfully", updated));
    }

    @GetMapping("/portfolio")
    @PreAuthorize("hasAnyAuthority('LANDLORD', 'AGENT', 'TENANT')")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getMyPortfolio(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        PortfolioResponse portfolio = propertyService.getUserPortfolio(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Real-time portfolio metrics retrieved successfully", portfolio));
    }
}