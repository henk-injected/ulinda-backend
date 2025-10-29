package org.ulinda.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ulinda.entities.Session;
import org.ulinda.services.SecuritySettingsService;
import org.ulinda.services.SessionService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class SessionChecker {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SecuritySettingsService securitySettingsService;

    @Scheduled(cron = "0 * * * * ?")
    public void runSessionChecker() {
        List<Session> sessions = sessionService.getAllSessions();
        int timeout = securitySettingsService.getSessionTimeoutMinutes();

        Instant now = Instant.now();
        for (Session session : sessions) {
            long minutesSinceLastAccess = Duration.between(session.getLastAccessed(), now).toMinutes();
            if (minutesSinceLastAccess >= timeout) {
                log.info("Invalidating Session: " + session.getId());
                try {
                    sessionService.deleteSession(session.getId());
                } catch (Exception e) {
                    log.error("Error while deleting session: " + session.getId(), e);
                }
            }
        }
    }
}
