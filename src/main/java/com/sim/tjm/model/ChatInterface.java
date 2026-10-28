package com.sim.tjm.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class ChatInterface {
    private boolean enabled;
    private String apiRef;
    private boolean sendActiveFilters;
    private String position;
    private String placeholder;
}
