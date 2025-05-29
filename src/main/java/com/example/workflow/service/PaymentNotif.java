package com.example.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Component
@Slf4j
public class PaymentNotif implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        long startTime = Instant.now().toEpochMilli();
        log.info("Send notification at {}", Instant.now().toString());

        for (int i = 0; i < 10; i++) {
            Map<String, Object> variables  = (Map<String, Object>) execution.getVariable("variables");

            log.info("variables {}", variables);
        }

        log.info("Send notification end at {}", Instant.now().toString());
        log.info("Send notification at {} ms", Instant.now().toEpochMilli() - startTime);

    }
}
