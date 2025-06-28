package com.example.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApprovalDecisionGateway implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        Boolean approved = (Boolean) execution.getVariable("approved");
        
        log.info("Auto-processing approval decision: {}", approved);
        
        // Auto-complete gateway decision
        if (approved != null && approved) {
            log.info("Approved - Auto-routing to Transfer");
            execution.setVariable("processStatus", "approved");
        } else {
            log.info("Rejected - Auto-routing to Submission Rejected");
            execution.setVariable("processStatus", "rejected");
        }
    }
}