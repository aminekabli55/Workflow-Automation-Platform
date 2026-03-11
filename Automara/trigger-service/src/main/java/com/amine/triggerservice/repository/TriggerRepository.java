package com.amine.triggerservice.repository;

import com.amine.triggerservice.model.Trigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TriggerRepository extends JpaRepository<Trigger, String> {

    List<Trigger> findByWorkflowId(String workflowId);

    List<Trigger> findByUserId(String userId);

    List<Trigger> findByStatus(String status);

    @Query("SELECT t FROM Trigger t WHERE t.triggerType = 'cron' " +
            "AND t.status = 'ACTIVE' " +
            "AND t.nextExecution <= :now")
    List<Trigger> findDueCronTriggers(LocalDateTime now);

    List<Trigger> findByTriggerType(String triggerType);
}