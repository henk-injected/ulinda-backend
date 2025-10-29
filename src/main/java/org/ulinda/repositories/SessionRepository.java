package org.ulinda.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import org.ulinda.entities.Session;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends ListCrudRepository<Session, UUID> {
    List<Session> findAllByUserId(UUID userId);
    void deleteAllByUserId(UUID userId);
}
