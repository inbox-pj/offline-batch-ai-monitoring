package com.cardconnect.bolt.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingData {
    private List<BatchMetrics> metrics;
    private HealthStatus label;
    private String description;
}
