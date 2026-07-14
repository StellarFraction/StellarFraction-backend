package com.example.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OfferRequest {
    @NotNull(message = "Property ID is required")
    private Long propertyId;

    @NotNull(message = "Offer amount is required")
    @Min(value = 1, message = "Offer amount must be greater than 0")
    private BigDecimal offerAmount;
}
