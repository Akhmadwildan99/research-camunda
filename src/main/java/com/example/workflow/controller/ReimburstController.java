package com.example.workflow.controller;

import com.example.workflow.Reimbursement;
import com.example.workflow.model.response.GenericResponse;
import com.example.workflow.service.CamundaService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reimbursement")
@Slf4j
public class ReimburstController {
    
    private final CamundaService camundaService;
    
    public ReimburstController(CamundaService camundaService) {
        this.camundaService = camundaService;
    }
    
    @PostMapping("/start")
    public ResponseEntity<GenericResponse<Map<String, Object>>> startReimbursement(@RequestBody Reimbursement reimbursement) {
        try {
            String businessKey = UUID.randomUUID().toString();
            reimbursement.setUuid(businessKey);
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("reimbursement", reimbursement);
            variables.put("amount", reimbursement.getAmount().doubleValue());
            variables.put("description", reimbursement.getDescription());
            
            ProcessInstance processInstance = camundaService.startWorkflow(
                "reimbusement-employee", 
                businessKey, 
                variables
            );
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("processInstanceId", processInstance.getProcessInstanceId());
            responseData.put("businessKey", businessKey);
            responseData.put("reimbursement", reimbursement);

            Task task = camundaService.getActiveTasksByBusinessKey(businessKey)
                    .stream()
                    .findFirst()
                    .orElse(null);;


            camundaService.completeTask(task.getId(), variables);
            
            return ResponseEntity.ok(new GenericResponse<>(200, "Reimbursement process started successfully", responseData));
            
        } catch (Exception e) {
            log.error("Error starting reimbursement process", e);
            return ResponseEntity.internalServerError()
                .body(new GenericResponse<>(500, "Failed to start reimbursement process: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/approve/manager/{businessKey}")
    public ResponseEntity<GenericResponse<String>> approveAsManager(@PathVariable("businessKey") String businessKey, @RequestParam("approved") boolean approved) {
        try {
            List<Task> tasks = camundaService.getActiveTasksByBusinessKey(businessKey);
            Task managerTask = tasks.stream()
                .filter(task -> "Manager Approval".equals(task.getName()))
                .findFirst()
                .orElse(null);
            
            if (managerTask == null) {
                return ResponseEntity.badRequest()
                    .body(new GenericResponse<>(400, "No Manager Approval task found for this reimbursement", null));
            }
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", approved);
            
            camundaService.completeTask(managerTask.getId(), variables);
            
            String message = approved ? "Manager approved the reimbursement (< 1M IDR)" : "Manager rejected the reimbursement";
            return ResponseEntity.ok(new GenericResponse<>(200, message, businessKey));
            
        } catch (Exception e) {
            log.error("Error in manager approval", e);
            return ResponseEntity.internalServerError()
                .body(new GenericResponse<>(500, "Failed to process manager approval: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/approve/director/{businessKey}")
    public ResponseEntity<GenericResponse<String>> approveAsDirector(@PathVariable("businessKey") String businessKey, @RequestParam("approved") boolean approved) {
        try {
            List<Task> tasks = camundaService.getActiveTasksByBusinessKey(businessKey);
            Task directorTask = tasks.stream()
                .filter(task -> "Director Approval".equals(task.getName()))
                .findFirst()
                .orElse(null);
            
            if (directorTask == null) {
                return ResponseEntity.badRequest()
                    .body(new GenericResponse<>(400, "No Director Approval task found for this reimbursement", null));
            }
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", approved);
            
            camundaService.completeTask(directorTask.getId(), variables);
            
            String message = approved ? "Director approved the reimbursement (>= 1M IDR)" : "Director rejected the reimbursement";
            return ResponseEntity.ok(new GenericResponse<>(200, message, businessKey));
            
        } catch (Exception e) {
            log.error("Error in director approval", e);
            return ResponseEntity.internalServerError()
                .body(new GenericResponse<>(500, "Failed to process director approval: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/rejection/{businessKey}")
    public ResponseEntity<GenericResponse<String>> handleRejection(@PathVariable("businessKey") String businessKey, @RequestParam("resubmit") boolean resubmit) {
        try {
            List<Task> tasks = camundaService.getActiveTasksByBusinessKey(businessKey);
            Task rejectionTask = tasks.stream()
                .filter(task -> "Submission rejected".equals(task.getName()))
                .findFirst()
                .orElse(null);
            
            if (rejectionTask == null) {
                return ResponseEntity.badRequest()
                    .body(new GenericResponse<>(400, "No rejection task found for this reimbursement", null));
            }
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("resubmit", resubmit);
            
            camundaService.completeTask(rejectionTask.getId(), variables);
            
            String message = resubmit ? "Reimbursement will be resubmitted" : "Reimbursement process ended";
            return ResponseEntity.ok(new GenericResponse<>(200, message, businessKey));
            
        } catch (Exception e) {
            log.error("Error handling rejection", e);
            return ResponseEntity.internalServerError()
                .body(new GenericResponse<>(500, "Failed to handle rejection: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/resubmit/{businessKey}")
    public ResponseEntity<GenericResponse<String>> resubmitReimbursement(@PathVariable("businessKey") String businessKey, @RequestBody Reimbursement reimbursement) {
        try {
            List<Task> tasks = camundaService.getActiveTasksByBusinessKey(businessKey);
            Task submissionTask = tasks.stream()
                .filter(task -> "Employee Submission".equals(task.getName()))
                .findFirst()
                .orElse(null);
            
            if (submissionTask == null) {
                return ResponseEntity.badRequest()
                    .body(new GenericResponse<>(400, "No Employee Submission task found for resubmission", null));
            }
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("reimbursement", reimbursement);
            variables.put("amount", reimbursement.getAmount().doubleValue());
            variables.put("description", reimbursement.getDescription());
            
            camundaService.completeTask(submissionTask.getId(), variables);
            
            return ResponseEntity.ok(new GenericResponse<>(200, "Reimbursement resubmitted successfully", businessKey));
            
        } catch (Exception e) {
            log.error("Error resubmitting reimbursement", e);
            return ResponseEntity.internalServerError()
                .body(new GenericResponse<>(500, "Failed to resubmit reimbursement: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/status/{businessKey}")
    public ResponseEntity<GenericResponse<Map<String, Object>>> getProcessStatus(@PathVariable("businessKey") String businessKey) {
        try {
            Map<String, Object> processStatus = camundaService.getProcessStatus(businessKey);
            
            if (processStatus.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(new GenericResponse<>(200, "Process status retrieved successfully", processStatus));
            
        } catch (Exception e) {
            log.error("Error getting process status", e);
            return ResponseEntity.internalServerError()
                .body(new GenericResponse<>(500, "Failed to get process status: " + e.getMessage(), null));
        }
    }
}
