package com.sim.tjm.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TjmDefinition {
    private String inputParameters;
    private String sourceTrigger;
    private String finalOutputEntity;
    private String finalOutputColumn;
    private List<Map<String, String>> finalResponseMappings;
    private List<TjmStep> tjmSequence;
}