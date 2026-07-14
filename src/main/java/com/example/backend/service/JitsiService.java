package com.example.backend.service;

import com.example.backend.dto.response.JitsiTokenResponse;

public interface JitsiService {
    JitsiTokenResponse generateRoomToken(Long propertyId, Long userId, String userName, String userEmail);
}
