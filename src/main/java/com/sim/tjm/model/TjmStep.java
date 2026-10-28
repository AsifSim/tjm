//package com.sim.tjm.model;
//
//import jakarta.persistence.Embeddable;
//import lombok.Data;
//import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.type.SqlTypes;
//
//@Data
//@Embeddable
//public class TjmStep {
//    private String stepType; // "FABRIC_FETCH" or "RULE_EXECUTION"
//    private String targetEntity; // e.g., "Fabric_Pricing_DEAM"
//    private String targetColumns;
//    // Add this field to your existing TjmStep.java class
//    @JdbcTypeCode(SqlTypes.JSON)
//    private java.util.List<java.util.Map<String, String>> stepMappings;
//}



package com.sim.tjm.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TjmStep {
    private String stepType;
    private String targetEntity;
    private String targetColumns;

    // Notice: NO @JdbcTypeCode, @Column, or @Entity annotations.
    // It is just a standard Java List now, handled entirely by Jackson during JSON conversion.
    private List<Map<String, String>> stepMappings;
}