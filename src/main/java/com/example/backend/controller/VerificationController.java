package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.SmileIdResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.AgentVerification;
import com.example.backend.model.KybVerification;
import com.example.backend.model.PropertyVerification;
import com.example.backend.model.User;
import com.example.backend.repository.AgentVerificationRepository;
import com.example.backend.repository.KybVerificationRepository;
import com.example.backend.repository.PropertyVerificationRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.DojahService;
import com.example.backend.service.OcrVerificationService;
import com.example.backend.service.StorageService;
import com.example.backend.model.PayoutVerification;
import com.example.backend.repository.PayoutVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.backend.model.Property;
import com.example.backend.repository.PropertyRepository;
import com.example.backend.util.ExifParser;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final KybVerificationRepository kybRepo;
    private final PropertyVerificationRepository propertyVerificationRepo;
    private final AgentVerificationRepository agentVerificationRepo;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PayoutVerificationRepository payoutVerificationRepo;
    private final DojahService dojahService;
    private final StorageService storageService;
    private final OcrVerificationService ocrVerificationService;

    @PostMapping("/kyb")
    public ResponseEntity<ApiResponse<KybVerification>> submitKyb(@RequestBody @org.springframework.lang.NonNull KybVerification kyb) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        if (kyb.getUser() == null || !authenticatedUser.getId().equals(kyb.getUser().getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You cannot submit KYB verification for another user."));
        }
        return ResponseEntity.ok(ApiResponse.success("KYB submitted successfully", kybRepo.save(kyb)));
    }

    @PostMapping("/property")
    public ResponseEntity<ApiResponse<PropertyVerification>> submitPropertyVerification(@RequestBody @org.springframework.lang.NonNull PropertyVerification pv) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        if (pv.getProperty() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Property must not be null"));
        }
        Property property = propertyRepository.findById(pv.getProperty().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        if (!property.getSeller().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not own this property listing."));
        }
        return ResponseEntity.ok(ApiResponse.success("Property verification submitted successfully", propertyVerificationRepo.save(pv)));
    }

    @PostMapping(value = "/property/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PropertyVerification>> verifyPropertyListing(
            @RequestParam("propertyId") @org.springframework.lang.NonNull Long propertyId,
            @RequestParam("proofImage") MultipartFile proofImage) {
        
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (!property.getSeller().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not own this property listing."));
        }

        // 1. Extract GPS coordinates from image EXIF metadata
        double[] coordinates = ExifParser.extractGpsCoordinates(proofImage);
        if (coordinates == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Verification failed: No GPS location coordinates found in the uploaded photograph's EXIF metadata. " +
                "Please capture the image with your phone camera's location settings enabled."
            ));
        }

        double photoLat = coordinates[0];
        double photoLng = coordinates[1];

        if (property.getLatitude() == null || property.getLongitude() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Verification failed: Property listing coordinates (latitude/longitude) are missing. " +
                "Please configure the listing's target physical coordinates first."
            ));
        }

        // 2. Calculate distance between photographer's GPS coordinates and property address
        double distance = calculateDistance(
            property.getLatitude(), property.getLongitude(),
            photoLat, photoLng
        );

        // Upload proof image to secure storage
        String imageUrl = storageService.uploadFile(proofImage, "properties/proof_" + propertyId);

        PropertyVerification pv = new PropertyVerification();
        pv.setProperty(property);
        pv.setProofOfOwnershipUrl(imageUrl);
        pv.setLatitude(photoLat);
        pv.setLongitude(photoLng);

        // 3. Enforce geofenced trust radius check: Must be under 20 meters!
        if (distance <= 20.0) {
            pv.setStatus(com.example.backend.model.VerificationStatus.APPROVED);
            property.setVerified(true);
            propertyRepository.save(property);
            PropertyVerification saved = propertyVerificationRepo.save(pv);
            
            return ResponseEntity.ok(ApiResponse.success(
                String.format("Property successfully verified! GPS location matches within safe trust radius (%.2f meters). Listing is now live.", distance),
                saved
            ));
        } else {
            pv.setStatus(com.example.backend.model.VerificationStatus.REJECTED);
            property.setVerified(false);
            propertyRepository.save(property);
            propertyVerificationRepo.save(pv);
            
            return ResponseEntity.badRequest().body(ApiResponse.error(
                String.format("Verification rejected: Photo captured location is too far from property coordinates (%.2f meters). Secure geofence limit is 20 meters.", distance)
            ));
        }
    }

    @PostMapping(value = "/property/verify-bill", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PropertyVerification>> verifyPropertyListingByBill(
            @RequestParam("propertyId") @org.springframework.lang.NonNull Long propertyId,
            @RequestParam("utilityBill") MultipartFile utilityBill) {

        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (!property.getSeller().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not own this property listing."));
        }

        String expectedName = property.getSeller().getFirstName() + " " + property.getSeller().getLastName();
        String expectedAddress = property.getAddress();

        // 1. Run Amazon Textract OCR matching analysis
        OcrVerificationService.OcrMatchResult ocrResult = ocrVerificationService.verifyUtilityBill(
                utilityBill, expectedName, expectedAddress
        );

        // Upload utility bill to secure storage
        String imageUrl = storageService.uploadFile(utilityBill, "properties/bills_" + propertyId);

        PropertyVerification pv = new PropertyVerification();
        pv.setProperty(property);
        pv.setProofOfOwnershipUrl(imageUrl);
        
        // Extract EXIF coordinates from bill image if present (as a fallback)
        double[] coordinates = ExifParser.extractGpsCoordinates(utilityBill);
        if (coordinates != null) {
            pv.setLatitude(coordinates[0]);
            pv.setLongitude(coordinates[1]);
        }

        if (ocrResult.isSuccess()) {
            pv.setStatus(com.example.backend.model.VerificationStatus.APPROVED);
            property.setVerified(true);
            propertyRepository.save(property);
            PropertyVerification saved = propertyVerificationRepo.save(pv);

            return ResponseEntity.ok(ApiResponse.success(
                "Property listing auto-verified and published successfully! " + ocrResult.getMessage(),
                saved
            ));
        } else {
            pv.setStatus(com.example.backend.model.VerificationStatus.REJECTED);
            property.setVerified(false);
            propertyRepository.save(property);
            propertyVerificationRepo.save(pv);

            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Utility bill verification rejected: " + ocrResult.getMessage()
            ));
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000; // Earth's radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    @PostMapping("/dojah/nin")
    public ResponseEntity<ApiResponse<SmileIdResponse>> verifyNin(
            @RequestParam String nin) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        SmileIdResponse response = dojahService.verifyNin(nin, authenticatedUser.getId());
        if (response.isSuccess()) {
            authenticatedUser.setIdentityVerified(true);
            userRepository.save(authenticatedUser);
            return ResponseEntity.ok(ApiResponse.success("NIN successfully verified via Dojah", response));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("NIN verification failed: " + response.getMessage()));
    }

    @PostMapping("/dojah/bvn")
    public ResponseEntity<ApiResponse<SmileIdResponse>> verifyBvn(
            @RequestParam String bvn) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        SmileIdResponse response = dojahService.verifyBvn(bvn, authenticatedUser.getId());
        if (response.isSuccess()) {
            authenticatedUser.setIdentityVerified(true);
            userRepository.save(authenticatedUser);
            return ResponseEntity.ok(ApiResponse.success("BVN successfully verified via Dojah", response));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("BVN verification failed: " + response.getMessage()));
    }

    @PostMapping(value = "/dojah/selfie", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SmileIdResponse>> verifySelfie(
            @RequestParam("selfie") MultipartFile selfie) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        SmileIdResponse response = dojahService.checkSelfieLiveness(selfie, authenticatedUser.getId());
        if (response.isSuccess()) {
            authenticatedUser.setIdentityVerified(true);
            userRepository.save(authenticatedUser);
            return ResponseEntity.ok(ApiResponse.success("Selfie liveness check succeeded via Dojah", response));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Selfie check failed: " + response.getMessage()));
    }

    @PostMapping(value = "/agent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AgentVerification>> submitAgentVerification(
            @RequestParam("selfie") MultipartFile selfie,
            @RequestParam("nin") String nin,
            @RequestParam("bvn") String bvn,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) {
        
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (authenticatedUser.getRole() != com.example.backend.model.Role.AGENT) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Only users with the AGENT role can submit agent verifications"));
        }

        // 1. Perform NIN verification check
        SmileIdResponse ninResponse = dojahService.verifyNin(nin, authenticatedUser.getId());
        if (!ninResponse.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Agent verification failed: NIN verification check failed - " + ninResponse.getMessage()));
        }

        // 2. Perform BVN verification check using Dojah
        SmileIdResponse bvnResponse = dojahService.verifyBvn(bvn, authenticatedUser.getId());
        if (!bvnResponse.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Agent verification failed: BVN verification check failed - " + bvnResponse.getMessage()));
        }

        // 3. Perform Selfie liveness check
        SmileIdResponse selfieResponse = dojahService.checkSelfieLiveness(selfie, authenticatedUser.getId());
        if (!selfieResponse.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Agent verification failed: Selfie biometric liveness check failed - " + selfieResponse.getMessage()));
        }

        // 4. Upload selfie image to secure S3 storage
        String selfieUrl = storageService.uploadFile(selfie, "selfies/agent_" + authenticatedUser.getId());

        AgentVerification verification = new AgentVerification();
        verification.setUser(authenticatedUser);
        verification.setNin(nin);
        verification.setBvn(bvn);
        verification.setSelfieUrl(selfieUrl);
        verification.setSmileTxId(selfieResponse.getSmileTxId());
        verification.setStatus(com.example.backend.model.VerificationStatus.APPROVED);
        verification.setLatitude(latitude);
        verification.setLongitude(longitude);

        authenticatedUser.setIdentityVerified(true);
        userRepository.save(authenticatedUser);

        AgentVerification saved = agentVerificationRepo.save(verification);

        return ResponseEntity.ok(ApiResponse.success("Agent identity checks passed. Verification successfully auto-approved via Dojah biometric trust.", saved));
    }

    @PostMapping("/payout/setup")
    public ResponseEntity<ApiResponse<PayoutVerification>> setupPayoutAccount(
            @RequestParam("bankCode") String bankCode,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("accountName") String accountName) {

        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        // Enforce that the account name contains the verified legal user name segments (First name & Last name)
        String userFirstName = authenticatedUser.getFirstName().toLowerCase().trim();
        String userLastName = authenticatedUser.getLastName().toLowerCase().trim();
        String lowerAccountName = accountName.toLowerCase().trim();

        if (!lowerAccountName.contains(userFirstName) || !lowerAccountName.contains(userLastName)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                String.format("Financial footprint check failed: The payout bank account name (%s) does not match the host's Dojah verified legal name (%s %s).", accountName, authenticatedUser.getFirstName(), authenticatedUser.getLastName())
            ));
        }

        // Generate two random micro-deposit verification codes (between 0.05 and 0.95)
        double micro1 = Math.round((Math.random() * 0.90 + 0.05) * 100.0) / 100.0;
        double micro2 = Math.round((Math.random() * 0.90 + 0.05) * 100.0) / 100.0;

        PayoutVerification pv = new PayoutVerification();
        pv.setUser(authenticatedUser);
        pv.setBankCode(bankCode);
        pv.setAccountNumber(accountNumber);
        pv.setAccountName(accountName);
        pv.setMicroDeposit1(micro1);
        pv.setMicroDeposit2(micro2);
        pv.setStatus(com.example.backend.model.VerificationStatus.PENDING);
        pv.setVerified(false);

        PayoutVerification saved = payoutVerificationRepo.save(pv);

        return ResponseEntity.ok(ApiResponse.success(
            "Payout bank account registered! Two micro-deposits have been dispatched to your bank account. Check your statement and verify the exact deposit values.",
            saved
        ));
    }

    @PostMapping("/payout/confirm")
    public ResponseEntity<ApiResponse<PayoutVerification>> confirmPayoutDeposits(
            @RequestParam("payoutId") @org.springframework.lang.NonNull Long payoutId,
            @RequestParam("deposit1") double deposit1,
            @RequestParam("deposit2") double deposit2) {

        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        PayoutVerification pv = payoutVerificationRepo.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout verification record not found"));

        if (!pv.getUser().getId().equals(authenticatedUser.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You do not own this payout verification record."));
        }

        // Match amounts order-independently for user convenience
        boolean exactMatch = (Math.abs(deposit1 - pv.getMicroDeposit1()) < 0.01 && Math.abs(deposit2 - pv.getMicroDeposit2()) < 0.01) ||
                             (Math.abs(deposit1 - pv.getMicroDeposit2()) < 0.01 && Math.abs(deposit2 - pv.getMicroDeposit1()) < 0.01);

        if (exactMatch) {
            pv.setStatus(com.example.backend.model.VerificationStatus.APPROVED);
            pv.setVerified(true);
            PayoutVerification saved = payoutVerificationRepo.save(pv);

            return ResponseEntity.ok(ApiResponse.success(
                "Micro-deposits successfully verified! Payout bank account activated for payouts.",
                saved
            ));
        } else {
            pv.setStatus(com.example.backend.model.VerificationStatus.REJECTED);
            payoutVerificationRepo.save(pv);

            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Payout verification failed: The submitted deposit amounts do not match the dispatched values."
            ));
        }
    }
}
