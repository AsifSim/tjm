package com.sim.tjm.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

// 2. FILTER_DEFINITIONS TABLES
@Data
@Entity
@Table(name = "filter_config")
public class FilterConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String apiRef;

    @ElementCollection
    @CollectionTable(name = "filter_content", joinColumns = @JoinColumn(name = "filter_config_id"))
    private List<FilterContent> content;
}
