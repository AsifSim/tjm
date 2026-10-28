package com.sim.tjm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.Data;

// 6. SHARED EMBEDDED CONFIGS (Lives inside App_Configuration table)
@Data
@Embeddable
public class ExcelPlugin {
    private boolean active;
    private String fileName;
    @JsonProperty("import")
    private boolean importMode;
}
