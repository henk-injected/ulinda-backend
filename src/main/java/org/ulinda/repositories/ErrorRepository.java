package org.ulinda.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.ulinda.entities.ErrorLog;

import java.util.UUID;

public interface ErrorRepository extends PagingAndSortingRepository<ErrorLog, UUID>, CrudRepository<ErrorLog, UUID> {
    ErrorLog findByErrorIdentifier(UUID errorId);
}
