package com.sim.tjm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

// 1. APP_CONFIGURATION TABLE
@Data
@Entity
@Table(name = "app_configuration")
public class AppConfiguration {
    
    @Id
    private String appId;
    
    @JsonProperty("dashboard-name")
    private String dashboardName;
    private boolean generatedFromPpt;
    private boolean demoMode;

    @Embedded
    private ExcelPlugin excelPlugin;

    @Embedded
    private ChatInterface chatInterface;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "filter_id")
    private FilterConfig filters;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "app_id") // Creates a foreign key in page_row table
    private List<PageRow> pageContent;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "faq_id")
    private FaqConfig faq;

    // Generates the API_ENTITY_REGISTRY table automatically
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(
            name = "app_apis", // Join table linking App to APIs
            joinColumns = @JoinColumn(name = "app_id"),
            inverseJoinColumns = @JoinColumn(name = "api_endpoint_id")
    )
    @MapKeyColumn(name = "api_key") // Stores the JSON keys like "customerViewFetch"
    private Map<String, ApiEndpoint> apis;
}

