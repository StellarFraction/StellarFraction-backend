package com.example.backend.service.impl;

import com.example.backend.dto.response.JitsiTokenResponse;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.*;
import com.example.backend.repository.CallSessionRepository;
import com.example.backend.repository.PropertyRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CallService;
import com.example.backend.service.JitsiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CallServiceImpl implements CallService {

    private final CallSessionRepository callSessionRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final JitsiService jitsiService;

    @Override
    @Transactional
    public CallSession initiateCall(Long callerId, Long receiverId, Long propertyId) {
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("Caller not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        // Set any existing pending call for this receiver to MISSED
        callSessionRepository.findByReceiverIdAndStatus(receiverId, CallStatus.INITIATED)
                .forEach(session -> {
                    session.setStatus(CallStatus.MISSED);
                    callSessionRepository.save(session);
                });

        JitsiTokenResponse jitsiInfo = jitsiService.generateRoomToken(
                propertyId,
                callerId,
                caller.getFirstName() + " " + caller.getLastName(),
                caller.getEmail()
        );

        CallSession session = CallSession.builder()
                .caller(caller)
                .receiver(receiver)
                .propertyId(propertyId)
                .status(CallStatus.INITIATED)
                .roomName(jitsiInfo.getRoomName())
                .jitsiToken(jitsiInfo.getToken())
                .joinUrl(jitsiInfo.getJoinUrl())
                .build();

        return callSessionRepository.save(session);
    }

    @Override
    @Transactional
    public CallSession acceptCall(Long callId, Long receiverId) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call session not found"));

        if (!session.getReceiver().getId().equals(receiverId)) {
            throw new RuntimeException("Access denied: You are not the receiver of this call");
        }

        if (session.getStatus() != CallStatus.INITIATED) {
            throw new RuntimeException("Call cannot be accepted. Current status: " + session.getStatus());
        }

        session.setStatus(CallStatus.ACCEPTED);
        
        User receiver = session.getReceiver();
        JitsiTokenResponse receiverJitsiInfo = jitsiService.generateRoomToken(
                session.getPropertyId(),
                receiverId,
                receiver.getFirstName() + " " + receiver.getLastName(),
                receiver.getEmail()
        );
        session.setJitsiToken(receiverJitsiInfo.getToken());
        session.setJoinUrl(receiverJitsiInfo.getJoinUrl());

        return callSessionRepository.save(session);
    }

    @Override
    @Transactional
    public CallSession rejectCall(Long callId, Long receiverId) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call session not found"));

        if (!session.getReceiver().getId().equals(receiverId)) {
            throw new RuntimeException("Access denied: You are not the receiver of this call");
        }

        if (session.getStatus() != CallStatus.INITIATED) {
            throw new RuntimeException("Call cannot be rejected. Current status: " + session.getStatus());
        }

        session.setStatus(CallStatus.REJECTED);
        return callSessionRepository.save(session);
    }

    @Override
    @Transactional
    public CallSession endCall(Long callId, Long userId) {
        CallSession session = callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call session not found"));

        if (!session.getCaller().getId().equals(userId) && !session.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("Access denied: You are not a participant in this call");
        }

        session.setStatus(CallStatus.ENDED);
        return callSessionRepository.save(session);
    }

    @Override
    public Optional<CallSession> getIncomingCall(Long receiverId) {
        return callSessionRepository.findTopByReceiverIdAndStatusOrderByCreatedAtDesc(receiverId, CallStatus.INITIATED);
    }

    @Override
    public CallSession getCallStatus(Long callId) {
        return callSessionRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call session not found"));
    }
}
