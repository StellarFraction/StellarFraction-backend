package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.JitsiTokenResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Property;
import com.example.backend.model.User;
import com.example.backend.repository.PropertyRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtService;
import com.example.backend.service.JitsiService;
import com.example.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class VirtualTourController {

    private final JitsiService jitsiService;
    private final StorageService storageService;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/jitsi/{propertyId}")
    public ResponseEntity<ApiResponse<JitsiTokenResponse>> getJitsiRoom(
            @PathVariable @org.springframework.lang.NonNull Long propertyId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        JitsiTokenResponse response = jitsiService.generateRoomToken(
                propertyId,
                java.util.Objects.requireNonNull(user.getId(), "User ID must not be null"),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail()
        );

        return ResponseEntity.ok(ApiResponse.success("Virtual tour room ready", response));
    }

    @PostMapping("/upload/{propertyId}")
    @PreAuthorize("hasAnyAuthority('LANDLORD', 'AGENT')")
    public ResponseEntity<ApiResponse<String>> uploadWalkthrough(
            @PathVariable @org.springframework.lang.NonNull Long propertyId,
            @RequestParam("file") @org.springframework.lang.NonNull MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (!property.getSeller().getId().equals(user.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not own this property listing."));
        }

        String videoUrl = storageService.uploadWalkthrough(file, propertyId);
        property.setVideoWalkthroughUrl(videoUrl);
        propertyRepository.save(property);

        return ResponseEntity.ok(ApiResponse.success("Video walkthrough uploaded successfully", videoUrl));
    }

    @PatchMapping("/matterport/{propertyId}")
    @PreAuthorize("hasAnyAuthority('LANDLORD', 'AGENT')")
    public ResponseEntity<ApiResponse<String>> setMatterportUrl(
            @PathVariable @org.springframework.lang.NonNull Long propertyId,
            @RequestParam @org.springframework.lang.NonNull String url,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (!property.getSeller().getId().equals(user.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not own this property listing."));
        }

        property.setVirtualTourUrl(url);
        propertyRepository.save(property);

        return ResponseEntity.ok(ApiResponse.success("3D virtual tour URL saved", url));
    }
}
