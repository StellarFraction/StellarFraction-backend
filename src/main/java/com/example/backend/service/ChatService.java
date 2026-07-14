package com.example.backend.service;

import com.example.backend.dto.request.ChatMessageRequest;
import com.example.backend.dto.response.ChatMessageResponse;

import java.util.List;

public interface ChatService {
    ChatMessageResponse sendMessage(@org.springframework.lang.NonNull Long senderId, ChatMessageRequest request);
    List<ChatMessageResponse> retrievePendingMessages(@org.springframework.lang.NonNull Long userId);
    long getPendingCount(@org.springframework.lang.NonNull Long userId);
}
