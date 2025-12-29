package com.cardconnect.bolt.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "offline_batch_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "merch_id", nullable = false, length = 20)
    private String merchId;

    @Column(name = "hsn", nullable = false, length = 50)
    private String hsn;

    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "processed_count", nullable = false)
    private Long processedCount;

    @Column(name = "error_count", nullable = false)
    private Long errorCount;

    @Column(name = "processing_time_ms", nullable = false)
    private Long processingTimeMs;

    @Column(name = "batch_size", nullable = false)
    private Integer batchSize;

    @Column(length = 50)
    private String status;

    @Column(name = "error_type", length = 50)
    private String errorType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
