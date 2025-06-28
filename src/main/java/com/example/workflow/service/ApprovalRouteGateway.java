package com.example.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApprovalRouteGateway implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        Double amount = (Double) execution.getVariable("amount");
        
        log.info("Auto-routing approval based on amount: {}", amount);
        
        // Auto-complete gateway routing
        if (amount >= 1000000) {
            log.info("Amount >= 1M IDR - Auto-routing to Director Approval");
            execution.setVariable("approvalRoute", "director");
        } else {
            log.info("Amount < 1M IDR - Auto-routing to Manager Approval");
            execution.setVariable("approvalRoute", "manager");
        }
    }
}