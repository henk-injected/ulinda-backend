package org.ulinda.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.ulinda.entities.SecuritySettings;

@Repository
public interface SecuritySettingsRepository extends CrudRepository<SecuritySettings, Integer> {
}
