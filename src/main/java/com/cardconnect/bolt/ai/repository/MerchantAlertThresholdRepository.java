package com.cardconnect.bolt.ai.repository;

import com.cardconnect.bolt.ai.model.merchant.MerchantAlertThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for merchant-specific alert thresholds
 */
@Repository
public interface MerchantAlertThresholdRepository extends JpaRepository<MerchantAlertThreshold, Long> {

    /**
     * Find threshold by merchant ID
     */
    Optional<MerchantAlertThreshold> findByMerchId(String merchId);

    /**
     * Find threshold by merchant ID and HSN
     */
    Optional<MerchantAlertThreshold> findByMerchIdAndHsn(String merchId, String hsn);

    /**
     * Find all thresholds for merchants with alerts enabled
     */
    List<MerchantAlertThreshold> findByAlertsEnabledTrue();

    /**
     * Find all thresholds ordered by priority
     */
    List<MerchantAlertThreshold> findAllByOrderByPriorityLevelDesc();

    /**
     * Find high-priority merchants
     */
    @Query("SELECT t FROM MerchantAlertThreshold t WHERE t.priorityLevel >= :minPriority ORDER BY t.priorityLevel DESC")
    List<MerchantAlertThreshold> findHighPriorityMerchants(@Param("minPriority") int minPriority);

    /**
     * Find merchants with email notifications enabled
     */
    List<MerchantAlertThreshold> findByEmailNotificationEnabledTrue();

    /**
     * Find merchants with Slack notifications enabled
     */
    List<MerchantAlertThreshold> findBySlackNotificationEnabledTrue();

    /**
     * Check if a merchant has custom thresholds
     */
    boolean existsByMerchId(String merchId);

    /**
     * Delete threshold by merchant ID
     */
    void deleteByMerchId(String merchId);

    /**
     * Find merchants by name pattern
     */
    @Query("SELECT t FROM MerchantAlertThreshold t WHERE LOWER(t.merchantName) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    List<MerchantAlertThreshold> findByMerchantNameContaining(@Param("namePattern") String namePattern);
}

