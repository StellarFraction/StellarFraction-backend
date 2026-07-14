package com.example.backend.service.impl;

import com.example.backend.dto.response.JitsiTokenResponse;
import com.example.backend.service.JitsiService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JitsiServiceImpl implements JitsiService {

    @Value("${jitsi.app-id}")
    private String appId;

    @Value("${jitsi.app-secret}")
    private String appSecret;

    @Value("${jitsi.server-url}")
    private String serverUrl;

    private static final long TOKEN_VALIDITY_MS = 2 * 60 * 60 * 1000L;

    @Override
    public JitsiTokenResponse generateRoomToken(Long propertyId, Long userId, String userName, String userEmail) {
        String roomName = "keyz-property-" + propertyId;

        Map<String, Object> userContext = new HashMap<>();
        userContext.put("name", userName);
        userContext.put("email", userEmail);
        userContext.put("id", userId.toString());

        Map<String, Object> context = new HashMap<>();
        context.put("user", userContext);

        Map<String, Object> claims = new HashMap<>();
        claims.put("context", context);
        claims.put("room", roomName);

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuer(appId)
                .setSubject(appId)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        String joinUrl = serverUrl + "/" + appId + "/" + roomName + "?jwt=" + token;

        return new JitsiTokenResponse(roomName, token, joinUrl);
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(appSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
