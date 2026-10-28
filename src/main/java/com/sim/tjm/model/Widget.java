package com.sim.tjm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "widget")
public class Widget {
    @Id
    private String widgetId;
    private String type;
    private String title;
    private String subtitle;

    @Column(name = "css_class") // "class" is a reserved keyword in Java
    @JsonProperty("class")
    private String cssClass;
    private String heightLg;
    private String apiRef;
    private String dataKey;
    private String categoryKey;
    private String valueKey;
    private String seriesKey;

    @Column(columnDefinition = "TEXT") // Store complex nested chart configs as JSON String for now
    private String chartConfig;
}
