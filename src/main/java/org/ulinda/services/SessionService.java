package org.ulinda.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ulinda.entities.Session;
import org.ulinda.repositories.SessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {
    @Autowired
    private SessionRepository sessionRepository;

    public UUID validateSessionId(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow(()->new IllegalArgumentException("Invalid session Id"));
        session.setLastAccessed(Instant.now());
        sessionRepository.save(session);
        return session.getUserId();
    }

    public UUID createSession(UUID userId, String ipAddress) {
        Session session = new Session();
        session.setUserId(userId);
        session.setIpAddr(ipAddress);
        Instant now = Instant.now();
        session.setCreatedAt(now);
        session.setLastAccessed(now);
        Session savedSession = sessionRepository.save(session);
        return savedSession.getId();
    }

    public void invalidateSession(UUID sessionId) {
        sessionRepository.findById(sessionId).orElseThrow(()->new IllegalArgumentException("Invalid session Id"));
        sessionRepository.deleteById(sessionId);
    }

    public void deleteSessionsForUser(UUID userId) {
        sessionRepository.deleteAllByUserId(userId);
    }

    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }

    @Transactional
    public void deleteSession(UUID sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}
