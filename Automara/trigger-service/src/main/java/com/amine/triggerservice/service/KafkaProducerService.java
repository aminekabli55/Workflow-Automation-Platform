package com.amine.triggerservice.service;

import com.amine.triggerservice.model.WorkflowTriggerEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaProducerService {

    private static final String TOPIC = "workflow-trigger";

    @Autowired
    private KafkaTemplate<String, WorkflowTriggerEvent> kafkaTemplate;

    public void sendTriggerEvent(WorkflowTriggerEvent event) {
        log.info("Sending trigger event for workflow: {} triggered by: {}",
                event.getWorkflowId(), event.getTriggeredBy());

        kafkaTemplate.send(TOPIC, event.getWorkflowId(), event);

        log.info("Trigger event sent successfully");
    }
}
