package com.cardconnect.bolt.ai.repository;

import com.cardconnect.bolt.ai.model.BatchMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfflineBatchMetricsRepository extends JpaRepository<BatchMetrics, Long> {

    List<BatchMetrics> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<BatchMetrics> findByMerchIdAndHsn(String merchId, String hsn);

    List<BatchMetrics> findByMerchId(String merchId);

    @Query("SELECT m FROM BatchMetrics m WHERE m.errorCount > 0 ORDER BY m.timestamp DESC")
    List<BatchMetrics> findAllWithErrors();

    @Query("SELECT COUNT(m) FROM BatchMetrics m WHERE m.timestamp > :since AND m.errorCount > 0")
    Long countErrorsSince(LocalDateTime since);

    void deleteByTimestampBefore(LocalDateTime timestamp);

    @Query("SELECT m FROM BatchMetrics m WHERE m.timestamp > :timestamp")
    List<BatchMetrics> findByTimestampAfter(@Param("timestamp") LocalDateTime timestamp);

    @Query("SELECT DISTINCT m.merchId FROM BatchMetrics m WHERE m.timestamp > :timestamp")
    List<String> findDistinctMerchIdsSince(@Param("timestamp") LocalDateTime timestamp);

    @Query("SELECT m FROM BatchMetrics m WHERE m.merchId = :merchId AND m.timestamp BETWEEN :start AND :end")
    List<BatchMetrics> findByMerchIdAndTimestampBetween(
            @Param("merchId") String merchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(m) FROM BatchMetrics m WHERE m.timestamp BETWEEN :start AND :end")
    Long countByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(m.errorCount) FROM BatchMetrics m WHERE m.timestamp BETWEEN :start AND :end")
    Long sumErrorCountByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO offline_batch_metrics (...) VALUES (...)", nativeQuery = true)
    void batchInsert(List<BatchMetrics> metrics);
}
