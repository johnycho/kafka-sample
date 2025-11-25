package com.sample.kafka.repository;

import com.sample.kafka.entity.HourlySalesResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HourlySalesResultRepository extends JpaRepository<HourlySalesResult, Long> {

    List<HourlySalesResult> findByProductNameOrderByWindowStartDesc(String productName);

    @Query("SELECT h FROM HourlySalesResult h WHERE h.windowStart >= :startTime ORDER BY h.windowStart DESC")
    List<HourlySalesResult> findRecentResults(LocalDateTime startTime);

    List<HourlySalesResult> findTop10ByOrderByCreatedAtDesc();
}

