package com.example.workflow.controller;

import com.example.workflow.model.response.GenericResponse;
import com.example.workflow.service.CamundaService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {

    private static  final String PROCESS_PAYMENT = "payment-process";
    private final CamundaService camundaService;
    private final TaskService taskService;
    private final HistoryService historyService;

    public PaymentController(CamundaService camundaService, TaskService taskService, HistoryService historyService) {
        this.camundaService = camundaService;
        this.taskService = taskService;
        this.historyService = historyService;
    }


    @PostMapping
    public ResponseEntity<?> startPay(@RequestBody Map<String, Object> payload) {
        long start = System.currentTimeMillis();
        log.info("Start payment process in time {}", Instant.now().toString());
        try {
            String paymentId = UUID.randomUUID().toString();
            Double amount = Double.parseDouble(payload.get("amount").toString());
            String orderId = (String) payload.get("orderId");

            Map<String, Object> variables = new HashMap<>();
            variables.put("variables", Map.of(
                    "paymentId", Map.of("value", paymentId),
                    "amount", Map.of("value", amount),
                    "orderId", Map.of("value", orderId)
            ));

            ProcessInstance processInstance = camundaService.startWorkflow(PROCESS_PAYMENT, paymentId, variables);

            Task tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getRootProcessInstanceId())
                    .list().stream().findFirst().orElse(null);
            assert tasks != null;
            camundaService.completeTask(tasks.getId(), variables);

            GenericResponse<String> g = new GenericResponse<String>(HttpStatus.OK.value(), "success", paymentId);
            long end = System.currentTimeMillis();
            log.info("End payment process in time {}", Instant.now().toString());
            log.info("Payment process in {} ms", end - start);
            return ResponseEntity.ok(g);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(e.getMessage());
        }

    }

    @GetMapping("/history/variables/business-key")
    public List<Map<String, Object>> getHistoryVariablesByBusinessKey(@RequestParam(name = "businessKey") String businessKey) {
        // Langkah 1: Dapatkan semua historic process instances dengan businessKey tertentu
        List<HistoricProcessInstance> processes = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();

        // Langkah 2: Untuk setiap process instance, dapatkan variabel historisnya
        for (HistoricProcessInstance process : processes) {
            String processInstanceId = process.getId();

            // Dapatkan semua variabel untuk process instance ini
            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();

            Map<String, Object> processVariables = new HashMap<>();
            processVariables.put("processInstanceId", processInstanceId);
            processVariables.put("businessKey", businessKey);
            processVariables.put("startTime", process.getStartTime());
            processVariables.put("endTime", process.getEndTime());

            Map<String, Object> variableMap = new HashMap<>();
            for (HistoricVariableInstance variable : variables) {
                variableMap.put(variable.getName(), variable.getValue());
            }

            processVariables.put("variables", variableMap);
            result.add(processVariables);
        }

        return result;
    }


    @GetMapping("/tasks/active")
    public List<Map<String, Object>> getActiveTasksByName(
            @RequestParam(name = "taskName", required = false) String taskName,
            @RequestParam(name = "exactMatch", defaultValue = "false") boolean exactMatch) {

        // Buat query untuk active tasks
        var taskQuery = taskService.createTaskQuery()
                .active(); // Hanya task yang aktif

        // Filter berdasarkan nama task jika disediakan
        if (taskName != null && !taskName.isEmpty()) {
            if (exactMatch) {
                // Match persis dengan nama task
                taskQuery.taskName(taskName);
            } else {
                // Menggunakan pattern matching (LIKE '%taskName%')
                taskQuery.taskNameLike("%" + taskName + "%");
            }
        }

        // Mendapatkan tasks dan mengkonversi ke format yang lebih mudah digunakan
        List<Task> tasks = taskQuery.orderByTaskCreateTime().desc().list();

        // Konversi Task objects ke Maps untuk output JSON yang lebih fleksibel
        return tasks.stream().map(task -> {
            Map<String, Object> taskMap = Map.of(
                    "id", task.getId(),
                    "name", task.getName(),
                    "description", Optional.ofNullable(task.getDescription()).orElse(""),
                    "assignee", Optional.ofNullable(task.getAssignee()).orElse(""),
                    "created", task.getCreateTime(),
                    "priority", task.getPriority(),
                    "processInstanceId", task.getProcessInstanceId(),
                    "processDefinitionId", task.getProcessDefinitionId(),
                    "taskDefinitionKey", task.getTaskDefinitionKey()
            );
            return taskMap;
        }).collect(Collectors.toList());
    }

    @PostMapping("/tasks/complete-query")
    public List<Map<String, Object>> getActiveTasksByNameAndComplteTAsk(
            @RequestParam(name = "taskName", required = false) String taskName,
            @RequestParam(name = "exactMatch", defaultValue = "false") boolean exactMatch) {

        // Buat query untuk active tasks
        var taskQuery = taskService.createTaskQuery()
                .active(); // Hanya task yang aktif

        // Filter berdasarkan nama task jika disediakan
        if (taskName != null && !taskName.isEmpty()) {
            if (exactMatch) {
                // Match persis dengan nama task
                taskQuery.taskName(taskName);
            } else {
                // Menggunakan pattern matching (LIKE '%taskName%')
                taskQuery.taskNameLike("%" + taskName + "%");
            }
        }

        // Mendapatkan tasks dan mengkonversi ke format yang lebih mudah digunakan
        List<Task> tasks = taskQuery.orderByTaskCreateTime().desc().list();

        tasks.forEach(task -> {
            taskService.complete(task.getId());
        });

        // Konversi Task objects ke Maps untuk output JSON yang lebih fleksibel
        return tasks.stream().map(task -> {
            Map<String, Object> taskMap = Map.of(
                    "id", task.getId(),
                    "name", task.getName(),
                    "description", Optional.ofNullable(task.getDescription()).orElse(""),
                    "assignee", Optional.ofNullable(task.getAssignee()).orElse(""),
                    "created", task.getCreateTime(),
                    "priority", task.getPriority(),
                    "processInstanceId", task.getProcessInstanceId(),
                    "processDefinitionId", task.getProcessDefinitionId(),
                    "taskDefinitionKey", task.getTaskDefinitionKey()
            );
            return taskMap;
        }).collect(Collectors.toList());
    }

}
