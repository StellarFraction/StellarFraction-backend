package com.example.backend.dto.response;

import com.example.backend.model.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class PortfolioResponse {
    private Long totalPropertiesCount;
    private Long activeListingsCount;
    private Double totalValueForSale;
    private Double expectedMonthlyRentalIncome;
    private Long pendingOffersCount;
    private List<Property> properties;
}
