package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JitsiTokenResponse {
    private String roomName;
    private String token;
    private String joinUrl;
}
