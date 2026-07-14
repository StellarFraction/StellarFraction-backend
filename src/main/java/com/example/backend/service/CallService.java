package com.example.backend.service;

import com.example.backend.model.CallSession;
import java.util.Optional;

public interface CallService {
    CallSession initiateCall(Long callerId, Long receiverId, Long propertyId);
    CallSession acceptCall(Long callId, Long receiverId);
    CallSession rejectCall(Long callId, Long receiverId);
    CallSession endCall(Long callId, Long userId);
    Optional<CallSession> getIncomingCall(Long receiverId);
    CallSession getCallStatus(Long callId);
}
