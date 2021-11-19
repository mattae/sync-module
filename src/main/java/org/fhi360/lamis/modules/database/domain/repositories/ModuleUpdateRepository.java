package org.fhi360.lamis.modules.database.domain.repositories;

import org.fhi360.lamis.modules.database.domain.entities.ModuleUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuleUpdateRepository extends JpaRepository<ModuleUpdate, Long> {

    Optional<ModuleUpdate> findByName(String name);

    List<ModuleUpdate> findByUninstallIsTrue();

    List<ModuleUpdate> findByInstallIsTrue();
}
