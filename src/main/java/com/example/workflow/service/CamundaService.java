package com.example.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CamundaService {


    private final RuntimeService runtimeService;

    private final HistoryService historyService;

    private TaskService taskService;

    public CamundaService(RuntimeService runtimeService, HistoryService historyService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.taskService = taskService;
    }

    public ProcessInstance startWorkflow(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                processDefinitionKey,
                businessKey,
                variables
        );
        log.info("Process instance started: {}", processInstance.getProcessInstanceId());
        return processInstance;
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        // Mengambil task berdasarkan ID
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task != null) {
            // Menyelesaikan task dengan variabel yang diberikan
            taskService.complete(taskId, variables);
            System.out.println("Task with ID " + taskId + " has been completed.");
        } else {
            System.out.println("Task with ID " + taskId + " could not be found.");
        }
    }


    public Task getTask(String taskId) {
        return taskService.createTaskQuery().taskId(taskId).singleResult();
    }
    
    public List<Task> getActiveTasksByBusinessKey(String businessKey) {
        return taskService.createTaskQuery()
                .processInstanceBusinessKey(businessKey)
                .active()
                .list();
    }
    
    public Map<String, Object> getProcessStatus(String businessKey) {
        Map<String, Object> result = new HashMap<>();
        
        // Get active tasks
        List<Task> activeTasks = getActiveTasksByBusinessKey(businessKey);
        result.put("activeTasks", activeTasks.stream().map(task -> {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getId());
            taskInfo.put("taskName", task.getName());
            taskInfo.put("assignee", task.getAssignee());
            taskInfo.put("createTime", task.getCreateTime());
            return taskInfo;
        }).toList());
        
        // Get historic tasks
        List<org.camunda.bpm.engine.history.HistoricTaskInstance> historicTasks = historyService
                .createHistoricTaskInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .asc()
                .list();
        
        result.put("completedTasks", historicTasks.stream().map(task -> {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getId());
            taskInfo.put("taskName", task.getName());
            taskInfo.put("assignee", task.getAssignee());
            taskInfo.put("startTime", task.getStartTime());
            taskInfo.put("endTime", task.getEndTime());
            return taskInfo;
        }).toList());
        
        return result;
    }

    public Map<String, Object> getHistoricVariablesByBusinessKey(String businessKey) {
        HistoricProcessInstance process = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .orderByProcessInstanceEndTime().desc() // Urutkan berdasarkan waktu selesai terbaru
                .listPage(0, 1) // Ambil hanya 1 result
                .stream()
                .findFirst()
                .orElse(null);

        if (process == null) {
            return new HashMap<>();
        }

        String processInstanceId = process.getId();

        // Dapatkan variabel untuk process instance ini
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        Map<String, Object> result = new HashMap<>();
        // Tambahkan metadata process
        result.put("_processInstanceId", processInstanceId);
        result.put("_startTime", process.getStartTime());
        result.put("_endTime", process.getEndTime());
        result.put("_state", process.getState());

        // Tambahkan variabel-variabel
        for (HistoricVariableInstance variable : variables) {
            result.put(variable.getName(), variable.getValue());
        }

        return result;
    }
}
