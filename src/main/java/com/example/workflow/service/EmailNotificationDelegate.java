package com.example.workflow.service;

import com.example.workflow.Reimbursement;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Reimbursement reimbursement = (Reimbursement) execution.getVariable("reimbursement");
        String transferStatus = (String) execution.getVariable("transferStatus");
        String transferMessage = (String) execution.getVariable("transferMessage");
        
//        log.info("Sending email notification for reimbursement: {}", reimbursement.g());
        log.info("Transfer Status: {}", transferStatus);
        log.info("Email sent: Reimbursement has been processed. {}",
               transferMessage);
    }
}