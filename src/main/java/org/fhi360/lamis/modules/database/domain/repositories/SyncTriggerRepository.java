package org.fhi360.lamis.modules.database.domain.repositories;

import org.fhi360.lamis.modules.database.domain.entities.SyncTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncTriggerRepository extends JpaRepository<SyncTrigger, Long> {
    Optional<SyncTrigger> findByTriggerId(String id);
}
