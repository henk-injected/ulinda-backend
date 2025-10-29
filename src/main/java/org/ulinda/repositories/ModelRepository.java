package org.ulinda.repositories;

import org.springframework.data.repository.CrudRepository;
import org.ulinda.entities.Model;

import java.util.UUID;

public interface ModelRepository extends CrudRepository<Model, UUID> {

}
