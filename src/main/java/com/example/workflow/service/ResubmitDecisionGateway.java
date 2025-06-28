package com.example.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResubmitDecisionGateway implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        Boolean resubmit = (Boolean) execution.getVariable("resubmit");
        
        log.info("Auto-processing resubmit decision: {}", resubmit);
        
        // Auto-complete gateway decision
        if (resubmit != null && resubmit) {
            log.info("Resubmit - Auto-routing back to Employee Submission");
            execution.setVariable("nextAction", "resubmit");
        } else {
            log.info("End Process - Auto-routing to End Event");
            execution.setVariable("nextAction", "end");
        }
    }
}