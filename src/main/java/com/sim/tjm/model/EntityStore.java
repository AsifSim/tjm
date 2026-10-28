package com.sim.tjm.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "entity_store")
public class EntityStore {
    @Id
    private String uuid;
    
    @Column(name = "entity_name")
    private String entityName;
    
    @Column(name = "is_enabled")
    private Boolean isEnabled;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(columnDefinition = "jsonb")
    private String payload;
}