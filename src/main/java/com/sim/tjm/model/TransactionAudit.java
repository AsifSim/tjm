package com.sim.tjm.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "transaction_audit")
public class TransactionAudit {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId; // Groups all recursive steps of a single execution
    private String appId;
    private String apiRef;
    private String sourceTrigger; // CONSOLE, UI, EXCEL, CHAT
    
    private String actionType; // e.g., "TJM_START", "FABRIC_FETCH", "RULE_EXECUTION"
    private String target; // The specific DEAM accessed
    
    @Column(columnDefinition = "TEXT")
    private String stateSnapshot; // Optional: JSON dump of the payload at this exact step
    
    private String timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = Instant.now().toString(); // Molecule-level timestamping
    }
}