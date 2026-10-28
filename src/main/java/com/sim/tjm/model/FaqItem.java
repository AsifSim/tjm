package com.sim.tjm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class FaqItem {
    private String question;
    @Column(columnDefinition = "TEXT")
    private String answer;
}