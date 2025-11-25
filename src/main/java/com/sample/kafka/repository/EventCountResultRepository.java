package com.sample.kafka.repository;

import com.sample.kafka.entity.EventCountResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventCountResultRepository extends JpaRepository<EventCountResult, Long> {

    List<EventCountResult> findByEventTypeOrderByWindowStartDesc(String eventType);

    List<EventCountResult> findTop10ByOrderByCreatedAtDesc();
}

