package com.sim.tjm.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

// 3. WIDGET_LAYOUTS TABLES
@Data
@Entity
@Table(name = "page_row")
public class PageRow {
    @Id 
    private String id; // e.g., "row1"

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "row_id")
    private List<Widget> columns;
}
