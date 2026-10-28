//package com.sim.tjm.model;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.type.SqlTypes;
//import java.util.List;
//import java.util.Map;
//
//@Data
//@Entity
//@Table(name = "api_endpoint")
//public class ApiEndpoint {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String apiUrl;
//    private String method;
//    private String inputParameters;
//    private String sourceTrigger;
//    private String finalOutputEntity;
//    private String finalOutputColumn;
//
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(columnDefinition = "jsonb")
//    private List<Map<String, String>> finalResponseMappings;
//
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(columnDefinition = "jsonb")
//    private List<TjmStep> tjmSequence;
//}

package com.sim.tjm.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "api_endpoint")
public class ApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String apiUrl;
    private String method;

    // Nests all the TJM inputs, triggers, mappings, and steps inside this one JSON object
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private TjmSequence tjmSequence;
}