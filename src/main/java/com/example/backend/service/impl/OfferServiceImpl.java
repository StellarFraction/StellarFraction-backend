package com.example.backend.service.impl;

import com.example.backend.dto.request.ChatMessageRequest;
import com.example.backend.dto.request.OfferRequest;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Offer;
import com.example.backend.model.OfferStatus;
import com.example.backend.model.Property;
import com.example.backend.model.PropertyStatus;
import com.example.backend.model.User;
import com.example.backend.repository.OfferRepository;
import com.example.backend.repository.PropertyRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ChatService;
import com.example.backend.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @Override
    public Offer createOffer(@org.springframework.lang.NonNull OfferRequest request, @org.springframework.lang.NonNull Long buyerId) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        Property property = propertyRepository.findById(java.util.Objects.requireNonNull(request.getPropertyId(), "Property ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (property.getStatus() != PropertyStatus.FOR_SALE) {
            throw new ConflictException("This property is not available for sale");
        }

        if (!property.isVerified()) {
            throw new ConflictException("This property has not been verified yet");
        }

        Offer offer = new Offer();
        offer.setBuyer(buyer);
        offer.setProperty(property);
        offer.setOfferAmount(request.getOfferAmount());
        offer.setStatus(OfferStatus.PENDING);

        Offer savedOffer = offerRepository.save(offer);

        // Auto-generate secure negotiation thread entry directly in the recipient's transient chat queue!
        try {
            ChatMessageRequest chatReq = new ChatMessageRequest();
            chatReq.setReceiverId(property.getSeller().getId());
            chatReq.setContent("SYSTEM_OFFER: I have made a formal offer of ₦" + request.getOfferAmount() + 
                    " on your property: '" + property.getTitle() + "' (ID: " + property.getId() + "). Let's negotiate here!");
            chatService.sendMessage(buyerId, chatReq);
        } catch (Exception e) {
            // Gracefully catch chat relay errors so offer submission itself never fails
            System.err.println("Failed to relay offer message to chat queue: " + e.getMessage());
        }

        return savedOffer;
    }
}
