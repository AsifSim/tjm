package com.sim.tjm.repo;

import com.sim.tjm.model.EntityStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EntityStoreRepository extends JpaRepository<EntityStore, String> {
    List<EntityStore> findByIsEnabledTrueAndIsActiveTrue();
}