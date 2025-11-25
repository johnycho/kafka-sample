package com.sample.kafka.repository;

import com.sample.kafka.entity.DailySalesResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailySalesResultRepository extends JpaRepository<DailySalesResult, Long> {

    List<DailySalesResult> findByCategoryOrderBySalesDateDesc(String category);

    List<DailySalesResult> findBySalesDate(LocalDate salesDate);

    List<DailySalesResult> findTop10ByOrderByCreatedAtDesc();
}

