package com.sim.tjm.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class FilterContent {
    private String name;
    private String key;
    private String options;
    private String type;
    private boolean isMultiSelect;
    private Boolean isfirstOptionDefault; 
    private boolean isQueryParam;
    private String defaultValue;
}
