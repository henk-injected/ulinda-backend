package org.ulinda.repositories;

import org.springframework.data.repository.CrudRepository;
import org.ulinda.entities.Field;

import java.util.List;
import java.util.UUID;

public interface FieldRepository extends CrudRepository<Field, UUID> {
    List<Field> findByModelId(UUID modelId);
}
