package com.example.workflow.service;

import com.example.workflow.Reimbursement;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransferDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Reimbursement reimbursement = (Reimbursement) execution.getVariable("reimbursement");
        Double amount = (Double) execution.getVariable("amount");
        
        log.info("Processing transfer for reimbursement: {} with amount: {}", 
                reimbursement.getUuid(), amount);
        
        // Simulate transfer process
        Thread.sleep(1000);
        
        execution.setVariable("transferStatus", "SUCCESS");
        execution.setVariable("transferMessage", "Transfer completed successfully for amount: " + amount);
        
        log.info("Transfer completed for reimbursement: {}", reimbursement.getUuid());
    }
}