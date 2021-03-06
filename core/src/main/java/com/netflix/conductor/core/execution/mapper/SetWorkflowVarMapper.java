package com.netflix.conductor.core.execution.mapper;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.execution.SystemTaskType;
import com.netflix.conductor.core.execution.TerminateWorkflowException;

public class SetWorkflowVarMapper implements TaskMapper {

	 Logger logger = LoggerFactory.getLogger(SetWorkflowVarMapper.class);

		@Override
		public List<Task> getMappedTasks(TaskMapperContext taskMapperContext) throws TerminateWorkflowException {
			logger.debug("TaskMapperContext {} in SetVariableMapper", taskMapperContext);
	        List<Task> tasksToBeScheduled = new LinkedList<>();
	        WorkflowTask taskToSchedule = taskMapperContext.getTaskToSchedule();
	        Workflow workflowInstance = taskMapperContext.getWorkflowInstance();
	        Map<String, Object> taskInput = taskMapperContext.getTaskInput();
	        String taskId = taskMapperContext.getTaskId();

	        //QQ why is the case value and the caseValue passed and caseOutput passes as the same ??
	        Task varTask = new Task();
	        varTask.setTaskType(SystemTaskType.SET_WORKFLOW_VAR.name());
	        varTask.setTaskDefName(SystemTaskType.SET_WORKFLOW_VAR.name());
	        varTask.setReferenceTaskName(taskToSchedule.getTaskReferenceName());
	        varTask.setWorkflowInstanceId(workflowInstance.getWorkflowId());
	        varTask.setWorkflowType(workflowInstance.getWorkflowName());
	        varTask.setCorrelationId(workflowInstance.getCorrelationId());
	        varTask.setScheduledTime(System.currentTimeMillis());
	        varTask.setInputData(taskInput);
	        varTask.setTaskId(taskId);
	        varTask.setStatus(Task.Status.IN_PROGRESS);
	        varTask.setWorkflowTask(taskToSchedule);
	        tasksToBeScheduled.add(varTask);
	        return tasksToBeScheduled;
		}


}
