package com.example.backend.service.impl;

import com.example.backend.dto.response.PortfolioResponse;
import com.example.backend.model.Offer;
import com.example.backend.model.OfferStatus;
import com.example.backend.model.Property;
import com.example.backend.model.PropertyStatus;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.OfferRepository;
import com.example.backend.repository.PropertyRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.PropertyService;
import com.example.backend.util.DisintermediationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final DisintermediationFilter disintermediationFilter;

    @Override
    public Property createProperty(@org.springframework.lang.NonNull Property property, @org.springframework.lang.NonNull Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        // Sanitize title and description to prevent direct offline contact sharing (platform leakage)
        property.setTitle(disintermediationFilter.sanitize(property.getTitle()));
        property.setDescription(disintermediationFilter.sanitize(property.getDescription()));

        if (seller.getRole() != Role.LANDLORD && seller.getRole() != Role.AGENT) {
            throw new RuntimeException("Only landlords or agents can create listings");
        }

        if (seller.getRole() == Role.LANDLORD && property.getStatus() != PropertyStatus.FOR_RENT) {
            throw new RuntimeException("Landlords can only create listings for rent");
        }

        if (seller.getRole() == Role.AGENT && property.getStatus() != PropertyStatus.FOR_SALE) {
            throw new RuntimeException("Agents can only create listings for sale");
        }

        property.setSeller(seller);
        
        // Automated Geofenced Proof-of-Presence Check
        if (property.getLatitude() != null && property.getLongitude() != null &&
            property.getProofLatitude() != null && property.getProofLongitude() != null) {
            
            double distance = calculateDistance(
                property.getLatitude(), property.getLongitude(),
                property.getProofLatitude(), property.getProofLongitude()
            );
            
            // Auto-approve and publish listing if proof matches listing coordinates within a 30-meter radius
            if (distance <= 30.0) {
                property.setVerified(true);
            } else {
                property.setVerified(false);
            }
        } else {
            property.setVerified(false); 
        }

        return propertyRepository.save(property);
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

    @Override
    public List<Property> getPropertiesForSale() {
        return propertyRepository.findByStatusAndIsVerifiedTrue(PropertyStatus.FOR_SALE);
    }

    @Override
    public List<Property> getPropertiesForRent() {
        return propertyRepository.findByStatusAndIsVerifiedTrue(PropertyStatus.FOR_RENT);
    }

    @Override
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    @Override
    public PortfolioResponse getUserPortfolio(@org.springframework.lang.NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.example.backend.exception.ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.TENANT) {
            // Fetch accepted offers submitted by this tenant (purchased properties)
            List<Offer> acceptedOffers = offerRepository.findByBuyerId(userId).stream()
                    .filter(o -> o.getStatus() == OfferStatus.ACCEPTED)
                    .toList();

            List<Property> purchasedProperties = acceptedOffers.stream()
                    .map(Offer::getProperty)
                    .toList();

            double totalValuePurchased = acceptedOffers.stream()
                    .mapToDouble(o -> o.getOfferAmount().doubleValue())
                    .sum();

            return PortfolioResponse.builder()
                    .totalPropertiesCount((long) purchasedProperties.size())
                    .activeListingsCount((long) purchasedProperties.size())
                    .totalValueForSale(totalValuePurchased) // Sum of successful purchase prices
                    .expectedMonthlyRentalIncome(0.0)
                    .pendingOffersCount(0L)
                    .properties(purchasedProperties)
                    .build();
        } else {
            // Landlord / Agent listings portfolio
            List<Property> properties = propertyRepository.findBySellerId(userId);

            long totalProperties = properties.size();
            long activeListings = properties.stream().filter(Property::isVerified).count();

            double totalValueForSale = properties.stream()
                    .filter(p -> p.getStatus() == PropertyStatus.FOR_SALE && p.getPrice() != null)
                    .mapToDouble(p -> p.getPrice().doubleValue())
                    .sum();

            double expectedMonthlyRentalIncome = properties.stream()
                    .filter(p -> p.getStatus() == PropertyStatus.FOR_RENT && p.getPrice() != null)
                    .mapToDouble(p -> p.getPrice().doubleValue())
                    .sum();

            long pendingOffers = properties.stream()
                    .flatMap(p -> offerRepository.findByPropertyId(p.getId()).stream())
                    .filter(o -> o.getStatus() == OfferStatus.PENDING)
                    .count();

            return PortfolioResponse.builder()
                    .totalPropertiesCount(totalProperties)
                    .activeListingsCount(activeListings)
                    .totalValueForSale(totalValueForSale)
                    .expectedMonthlyRentalIncome(expectedMonthlyRentalIncome)
                    .pendingOffersCount(pendingOffers)
                    .properties(properties)
                    .build();
        }
    }

    @Override
    public Property getPropertyById(@org.springframework.lang.NonNull Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new com.example.backend.exception.ResourceNotFoundException("Property not found"));
    }

    @Override
    public Property updateProperty(@org.springframework.lang.NonNull Long id, @org.springframework.lang.NonNull Property propertyDetails, @org.springframework.lang.NonNull Long sellerId) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new com.example.backend.exception.ResourceNotFoundException("Property not found"));

        if (!property.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Access denied: You do not own this property listing.");
        }

        if (propertyDetails.getTitle() != null) {
            property.setTitle(disintermediationFilter.sanitize(propertyDetails.getTitle()));
        }
        if (propertyDetails.getDescription() != null) {
            property.setDescription(disintermediationFilter.sanitize(propertyDetails.getDescription()));
        }
        if (propertyDetails.getAddress() != null) {
            property.setAddress(propertyDetails.getAddress());
        }
        if (propertyDetails.getPrice() != null) {
            property.setPrice(propertyDetails.getPrice());
        }
        if (propertyDetails.getStatus() != null) {
            User seller = property.getSeller();
            if (seller.getRole() == Role.LANDLORD && propertyDetails.getStatus() != PropertyStatus.FOR_RENT) {
                throw new RuntimeException("Landlords can only list properties for rent");
            }
            if (seller.getRole() == Role.AGENT && propertyDetails.getStatus() != PropertyStatus.FOR_SALE) {
                throw new RuntimeException("Agents can only list properties for sale");
            }
            property.setStatus(propertyDetails.getStatus());
        }
        property.setBedrooms(propertyDetails.getBedrooms());
        property.setBathrooms(propertyDetails.getBathrooms());
        property.setSquareFootage(propertyDetails.getSquareFootage());
        if (propertyDetails.getLatitude() != null) {
            property.setLatitude(propertyDetails.getLatitude());
        }
        if (propertyDetails.getLongitude() != null) {
            property.setLongitude(propertyDetails.getLongitude());
        }
        if (propertyDetails.getVirtualTourUrl() != null) {
            property.setVirtualTourUrl(propertyDetails.getVirtualTourUrl());
        }
        if (propertyDetails.getVideoWalkthroughUrl() != null) {
            property.setVideoWalkthroughUrl(propertyDetails.getVideoWalkthroughUrl());
        }

        // Re-run geofence checks if listing physical coordinates are modified
        if (property.getLatitude() != null && property.getLongitude() != null &&
            property.getProofLatitude() != null && property.getProofLongitude() != null) {
            double distance = calculateDistance(
                property.getLatitude(), property.getLongitude(),
                property.getProofLatitude(), property.getProofLongitude()
            );
            property.setVerified(distance <= 30.0);
        }

        return propertyRepository.save(property);
    }
}
