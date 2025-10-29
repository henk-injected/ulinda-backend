package org.ulinda.repositories;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.ulinda.entities.ModelLink;

import java.util.List;
import java.util.UUID;

public interface ModelLinkRepository extends CrudRepository<ModelLink, UUID> {
    /**
     * Find all model links where the given modelId appears as either model_1_id or model_2_id
     *
     * @param modelId the UUID to search for in either model_1_id or model_2_id columns
     * @return List of ModelLink entities where the modelId is found
     */
    @Query("SELECT * FROM model_links WHERE model_1_id = :modelId OR model_2_id = :modelId")
    List<ModelLink> findByEitherModelId(@Param("modelId") UUID modelId);
}
