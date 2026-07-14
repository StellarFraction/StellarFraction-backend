package com.example.backend.service.impl;

import com.example.backend.dto.request.ChatMessageRequest;
import com.example.backend.dto.response.ChatMessageResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.ChatMessage;
import com.example.backend.model.User;
import com.example.backend.repository.ChatMessageRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ChatService;
import com.example.backend.util.DisintermediationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DisintermediationFilter disintermediationFilter;

    @Override
    public ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        User receiver = userRepository.findById(java.util.Objects.requireNonNull(request.getReceiverId(), "Receiver ID must not be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        
        // Sanitize raw text to prevent platform disintermediation/leakage
        String sanitizedContent = disintermediationFilter.sanitize(request.getContent());
        message.setContent(sanitizedContent);
        message.setTimestamp(LocalDateTime.now());

        ChatMessage saved = chatMessageRepository.save(message);

        return mapToResponse(saved);
    }

    @Override
    public List<ChatMessageResponse> retrievePendingMessages(Long userId) {
        List<ChatMessage> dbMessages = chatMessageRepository.findByReceiverId(userId);
        List<ChatMessageResponse> messages = new ArrayList<>();
        for (ChatMessage msg : dbMessages) {
            messages.add(mapToResponse(msg));
        }
        chatMessageRepository.deleteAll(dbMessages);
        return messages;
    }

    @Override
    public long getPendingCount(Long userId) {
        return chatMessageRepository.findByReceiverId(userId).size();
    }

    private ChatMessageResponse mapToResponse(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getFirstName() + " " + msg.getSender().getLastName())
                .receiverId(msg.getReceiver().getId())
                .receiverName(msg.getReceiver().getFirstName() + " " + msg.getReceiver().getLastName())
                .content(msg.getContent())
                .timestamp(msg.getTimestamp())
                .isRead(false)
                .build();
    }
}
