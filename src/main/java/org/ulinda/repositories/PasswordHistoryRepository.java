package org.ulinda.repositories;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import org.ulinda.entities.PasswordHistory;

import java.util.List;
import java.util.UUID;

@Repository
public interface PasswordHistoryRepository extends ListCrudRepository<PasswordHistory, UUID> {
    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
    void deleteByUserId(UUID userId);
}
