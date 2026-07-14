package com.example.backend;

import com.example.backend.dto.request.*;
import com.example.backend.dto.response.*;
import com.example.backend.model.*;
import com.example.backend.model.Role;
import com.example.backend.repository.*;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.PropertyRepository;
import com.example.backend.service.*;
import com.example.backend.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EndpointIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepo;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private DojahService dojahService;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private JitsiService jitsiService;

    @MockitoBean
    private OcrVerificationService ocrVerificationService;

    private String landlordToken;
    private String tenantToken;
    private String agentToken;

    private Long landlordId;
    private Long tenantId;
    private Long agentId;

    @BeforeEach
    public void setup() throws Exception {
        long timestamp = System.currentTimeMillis() + (long)(Math.random() * 10000);
        
        // Mock Dojah service methods so they don't hit production APIs
        Mockito.when(dojahService.verifyNin(anyString(), anyLong()))
               .thenReturn(SmileIdResponse.builder()
                       .success(true)
                       .status("VERIFIED")
                       .smileTxId("TX-NIN-123")
                       .build());
        Mockito.when(dojahService.verifyBvn(anyString(), anyLong()))
               .thenReturn(SmileIdResponse.builder()
                       .success(true)
                       .status("VERIFIED")
                       .smileTxId("TX-BVN-123")
                       .build());
        Mockito.when(dojahService.checkSelfieLiveness(any(), anyLong()))
               .thenReturn(SmileIdResponse.builder()
                       .success(true)
                       .status("VERIFIED")
                       .smileTxId("TX-SELFIE-123")
                       .build());

        // Mock Jitsi Room token generation
        Mockito.when(jitsiService.generateRoomToken(anyLong(), anyLong(), anyString(), anyString()))
               .thenReturn(new JitsiTokenResponse("room-123", "token-123", "https://meet.jit.si/room-123"));

        // 1. Register and Login a Landlord
        landlordId = createVerifiedUser("landlord_" + timestamp + "@example.com", Role.LANDLORD);
        landlordToken = loginUser("landlord_" + timestamp + "@example.com");

        // 2. Register and Login a Tenant
        tenantId = createVerifiedUser("tenant_" + timestamp + "@example.com", Role.TENANT);
        tenantToken = loginUser("tenant_" + timestamp + "@example.com");

        // 3. Register and Login an Agent
        agentId = createVerifiedUser("agent_" + timestamp + "@example.com", Role.AGENT);
        agentToken = loginUser("agent_" + timestamp + "@example.com");
    }

    private Long createVerifiedUser(String email, Role role) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
        registerRequest.setEmail(email);
        registerRequest.setPassword("SecurePassword123!");
        registerRequest.setRole(role);

        // Call register endpoint
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
                .header("X-Device-Fingerprint", "fingerprint-123")
                .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isOk());

        // Fetch User and verification token to verify the email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AssertionError("User registration failed"));

        EmailVerificationToken token = emailVerificationTokenRepo.findByUserId(user.getId())
                .orElseThrow(() -> new AssertionError("Verification token not generated"));

        // Verify email
        mockMvc.perform(post("/api/auth/verify-email")
                .param("email", email)
                .param("token", token.getToken()))
                .andExpect(status().isOk());

        // Mark identity as verified for test convenience
        user = userRepository.findById(user.getId()).orElseThrow();
        user.setIdentityVerified(true);
        userRepository.save(user);

        return user.getId();
    }

    private String loginUser(String email) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("SecurePassword123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("X-Device-Fingerprint", "fingerprint-123")
                .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseString);
        return "Bearer " + root.path("data").path("accessToken").asText();
    }

    @Test
    public void testPropertyListingsAndPortfolio() throws Exception {
        // 1. Create a Rental Property as a Landlord (Auto-verified via matching GPS)
        Property property = new Property();
        property.setTitle("Luxury Oceanview Penthouse");
        property.setDescription("Spacious penthouse with Jitsi virtual tours.");
        property.setAddress("12 Admiralty Way, Lekki");
        property.setPrice(BigDecimal.valueOf(450000.00));
        property.setBedrooms(4);
        property.setBathrooms(5);
        property.setSquareFootage(3800.0);
        property.setStatus(PropertyStatus.FOR_RENT);
        property.setLatitude(6.4281);
        property.setLongitude(3.4219);
        property.setProofLatitude(6.4281);
        property.setProofLongitude(3.4219);

        User seller = new User();
        seller.setId(landlordId);
        property.setSeller(seller);

        MvcResult createResult = mockMvc.perform(post("/api/properties/create")
                .header("Authorization", landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(property)))
                .andExpect(status().isOk())
                .andReturn();

        String createResp = createResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(createResp);
        Long propertyId = rootNode.path("data").path("id").asLong();
        assertNotNull(propertyId);

        // 2. Fetch sale/rent properties
        mockMvc.perform(get("/api/properties/all"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/properties/rent"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/properties/sale"))
                .andExpect(status().isOk());

        // 3. Fetch Landlord Portfolio
        mockMvc.perform(get("/api/properties/portfolio")
                .header("Authorization", landlordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPropertiesCount").value(1))
                .andExpect(jsonPath("$.data.activeListingsCount").value(1))
                .andExpect(jsonPath("$.data.expectedMonthlyRentalIncome").value(450000.00));
    }

    @Test
    public void testZeroKnowledgeChatMessaging() throws Exception {
        // Send a secure AES-encrypted message from Landlord to Tenant
        ChatMessageRequest chatRequest = new ChatMessageRequest();
        chatRequest.setReceiverId(tenantId);
        chatRequest.setContent("AESEncryptedMessageBodyHere==");

        mockMvc.perform(post("/api/chat/send")
                .header("Authorization", landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk());

        // Check pending message queue size
        mockMvc.perform(get("/api/chat/pending")
                .header("Authorization", tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // Consume and instantly self-purge the message
        mockMvc.perform(get("/api/chat/receive")
                .header("Authorization", tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("AESEncryptedMessageBodyHere=="));

        // Verify the queue is now empty (purged)
        mockMvc.perform(get("/api/chat/pending")
                .header("Authorization", tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    public void testOffersAndSystemNegotiations() throws Exception {
        // 1. Create a For-Sale Property as an Agent
        Property property = new Property();
        property.setTitle("Luxury Ikoyi Duplex");
        property.setDescription("Fitted with smart-home systems.");
        property.setAddress("4 Waterfront Road, Ikoyi");
        property.setPrice(BigDecimal.valueOf(150000000.00));
        property.setBedrooms(5);
        property.setBathrooms(6);
        property.setSquareFootage(4500.0);
        property.setStatus(PropertyStatus.FOR_SALE);
        property.setLatitude(6.4281);
        property.setLongitude(3.4219);
        property.setProofLatitude(6.4281);
        property.setProofLongitude(3.4219);

        User seller = new User();
        seller.setId(agentId);
        property.setSeller(seller);

        MvcResult createResult = mockMvc.perform(post("/api/properties/create")
                .header("Authorization", agentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(property)))
                .andExpect(status().isOk())
                .andReturn();

        Long propertyId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Submit a formal offer as a Tenant
        OfferRequest offerRequest = new OfferRequest();
        offerRequest.setPropertyId(propertyId);
        offerRequest.setOfferAmount(BigDecimal.valueOf(140000000.00));

        mockMvc.perform(post("/api/offers")
                .header("Authorization", tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(offerRequest))
                .param("buyerId", tenantId.toString()))
                .andExpect(status().isOk());

        // 3. Confirm that the Zero-Knowledge Chat service automatically queued a System Negotiation message in Agent's mailbox
        mockMvc.perform(get("/api/chat/receive")
                .header("Authorization", agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").isNotEmpty());
    }

    @Test
    public void testBookingsAndReviews() throws Exception {
        // 1. Create a For-Rent Property as a Landlord
        Property property = new Property();
        property.setTitle("Lekki 3-Bed Apartment");
        property.setAddress("12 Admiralty Way, Lekki");
        property.setPrice(BigDecimal.valueOf(300000.00));
        property.setBedrooms(3);
        property.setBathrooms(3);
        property.setSquareFootage(2100.0);
        property.setStatus(PropertyStatus.FOR_RENT);
        property.setLatitude(6.4281);
        property.setLongitude(3.4219);
        property.setProofLatitude(6.4281);
        property.setProofLongitude(3.4219);

        User seller = new User();
        seller.setId(landlordId);
        property.setSeller(seller);

        MvcResult createResult = mockMvc.perform(post("/api/properties/create")
                .header("Authorization", landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(property)))
                .andExpect(status().isOk())
                .andReturn();

        Long propertyId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Schedule a rental booking
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setPropertyId(propertyId);
        bookingRequest.setStartDate(java.time.LocalDate.now().plusDays(2));
        bookingRequest.setEndDate(java.time.LocalDate.now().plusDays(9));

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                .header("Authorization", tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest))
                .param("buyerId", tenantId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        Long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 3. Force-complete the booking so that the tenant can leave a review
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AssertionError("Booking not found in repository"));
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        // 4. Submit a review for the property
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setPropertyId(propertyId);
        reviewRequest.setRating(5);
        reviewRequest.setComment("Absolutely stunning property! Highly recommended!");

        mockMvc.perform(post("/api/reviews")
                .header("Authorization", tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest))
                .param("reviewerId", tenantId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    public void testVirtualToursAndSmileIdVerification() throws Exception {
        // 1. Create a Property
        Property property = new Property();
        property.setTitle("Victoria Island Condo");
        property.setAddress("4 Waterfront Road, Ikoyi");
        property.setPrice(BigDecimal.valueOf(500000.00));
        property.setStatus(PropertyStatus.FOR_RENT);
        property.setLatitude(6.4281);
        property.setLongitude(3.4219);
        property.setProofLatitude(6.4281);
        property.setProofLongitude(3.4219);

        User seller = new User();
        seller.setId(landlordId);
        property.setSeller(seller);

        MvcResult createResult = mockMvc.perform(post("/api/properties/create")
                .header("Authorization", landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(property)))
                .andExpect(status().isOk())
                .andReturn();

        Long propertyId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Request a Jitsi Room Token
        mockMvc.perform(post("/api/tours/jitsi/" + propertyId)
                .header("Authorization", landlordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomName").value("room-123"))
                .andExpect(jsonPath("$.data.token").value("token-123"));

        // 3. Perform Dojah verification checks
        mockMvc.perform(post("/api/verification/dojah/nin")
                .header("Authorization", landlordToken)
                .param("nin", "12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));

        mockMvc.perform(post("/api/verification/dojah/bvn")
                .header("Authorization", landlordToken)
                .param("bvn", "12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    public void testGetUserProfileRestrictions() throws Exception {
        String landlordEmail = userRepository.findById(landlordId).orElseThrow().getEmail();
        
        // Landlord should be able to get their own profile via /me
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", landlordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(landlordEmail))
                .andExpect(jsonPath("$.data.id").value(landlordId));

        // Landlord should be able to get their own profile
        mockMvc.perform(get("/api/users/" + landlordId)
                .header("Authorization", landlordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(landlordEmail));

        // Landlord should NOT be able to get Tenant's profile (403 Forbidden)
        mockMvc.perform(get("/api/users/" + tenantId)
                .header("Authorization", landlordToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testLogoutAndTokenBlacklisting() throws Exception {
        // Logout using Landlord token
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", landlordToken))
                .andExpect(status().isOk());

        // Subsequent requests with the blacklisted token should fail with 401 Unauthorized
        mockMvc.perform(get("/api/users/" + landlordId)
                .header("Authorization", landlordToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testChatMessagePersistence() throws Exception {
        // Send a message from Landlord to Tenant
        com.example.backend.dto.request.ChatMessageRequest request = new com.example.backend.dto.request.ChatMessageRequest();
        request.setReceiverId(tenantId);
        request.setContent("Hello, this is a persisted message");

        mockMvc.perform(post("/api/chat/send")
                .header("Authorization", landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Retrieve messages as Tenant (should see it)
        mockMvc.perform(get("/api/chat/receive")
                .header("Authorization", tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Hello, this is a persisted message"));

        // Retrieve again as Tenant (should see empty array because it was consumed/wiped)
        mockMvc.perform(get("/api/chat/receive")
                .header("Authorization", tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    public void testChangePassword() throws Exception {
        String landlordEmail = userRepository.findById(landlordId).orElseThrow().getEmail();

        com.example.backend.dto.request.ChangePasswordRequest request = new com.example.backend.dto.request.ChangePasswordRequest();
        request.setOldPassword("SecurePassword123!");
        request.setNewPassword("NewSecurePassword123!");

        // Change password
        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify that we can log in with the new password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(landlordEmail);
        loginRequest.setPassword("NewSecurePassword123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .header("X-Device-Fingerprint", "fingerprint-123")
                .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isOk());
    }

    @Test
    public void testAccountDeletionAndCompliance() throws Exception {
        mockMvc.perform(delete("/api/auth/account")
                .header("Authorization", agentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + agentId)
                .header("Authorization", agentToken))
                .andExpect(status().isUnauthorized());

        assertFalse(userRepository.findById(agentId).isPresent());
    }

    @Test
    public void testCallSignalingSetup() throws Exception {
        Property property = new Property();
        property.setTitle("Victoria Island Call Test Condo");
        property.setAddress("4 Waterfront Road, Ikoyi");
        property.setPrice(BigDecimal.valueOf(500000.00));
        property.setStatus(PropertyStatus.FOR_RENT);
        property.setLatitude(6.4281);
        property.setLongitude(3.4219);
        property.setProofLatitude(6.4281);
        property.setProofLongitude(3.4219);

        User seller = new User();
        seller.setId(landlordId);
        property.setSeller(seller);

        MvcResult createResult = mockMvc.perform(post("/api/properties/create")
                .header("Authorization", landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(property)))
                .andExpect(status().isOk())
                .andReturn();

        Long propertyId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult initiateResult = mockMvc.perform(post("/api/calls/initiate")
                .header("Authorization", tenantToken)
                .param("receiverId", landlordId.toString())
                .param("propertyId", propertyId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        Long callId = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).path("data").path("id").asLong();
        assertNotNull(callId);

        mockMvc.perform(get("/api/calls/incoming")
                .header("Authorization", landlordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(callId))
                .andExpect(jsonPath("$.data.status").value("INITIATED"));

        mockMvc.perform(post("/api/calls/" + callId + "/accept")
                .header("Authorization", landlordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/calls/" + callId + "/status")
                .header("Authorization", tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/calls/" + callId + "/end")
                .header("Authorization", tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"));
    }
}
