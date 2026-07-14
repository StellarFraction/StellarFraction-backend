package com.example.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users")
@Getter @Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String firstName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; 
    
    @Column(columnDefinition = "double precision default 0.0")
    private Double sellerRating = 0.0;

    @Column(name = "is_email_verified", nullable = true)
    private Boolean isEmailVerified = false;

    @Column(name = "is_identity_verified", nullable = true)
    private Boolean isIdentityVerified = false;

    public boolean isEmailVerified() {
        return isEmailVerified != null && isEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.isEmailVerified = emailVerified;
    }

    public boolean isIdentityVerified() {
        return isIdentityVerified != null && isIdentityVerified;
    }

    public void setIdentityVerified(boolean identityVerified) {
        this.isIdentityVerified = identityVerified;
    }

    @PrePersist
    @PreUpdate
    public void normalizeEmail() {
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }
}