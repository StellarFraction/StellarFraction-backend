package com.example.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@Entity
@Getter @Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String address;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    private int bedrooms;
    private int bathrooms;
    private double squareFootage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status; 

    @Column(nullable = false)
    private boolean isVerified = false;

    private String virtualTourUrl;

    private String videoWalkthroughUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    private Double latitude;
    private Double longitude;
    private Double proofLatitude;
    private Double proofLongitude;

    private String imageUrl;
    private String imageHash;
    private boolean isFlaggedAsDuplicate = false;
}