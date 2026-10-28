package com.sim.tjm.repo;

import com.sim.tjm.model.AppConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppConfigurationRepository extends JpaRepository<AppConfiguration, String> {
    // Basic CRUD operations (findById, save) are automatically provided
}