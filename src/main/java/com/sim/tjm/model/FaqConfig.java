package com.sim.tjm.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

// 4. FAQ_REPOSITORY TABLES
@Data
@Entity
@Table(name = "faq_config")
public class FaqConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private boolean enabled;
    private String apiRef;

    @ElementCollection
    @CollectionTable(name = "faq_assistants")
    private Map<String, Boolean> assistants;

    @ElementCollection
    @CollectionTable(name = "faq_items", joinColumns = @JoinColumn(name = "faq_config_id"))
    private List<FaqItem> items;
}
