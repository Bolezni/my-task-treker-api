package task_tracker_api.api.controllers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import task_tracker_api.api.controllers.helpers.ControllerHelper;
import task_tracker_api.api.dto.AckDto;
import task_tracker_api.api.dto.TaskDto;
import task_tracker_api.api.exceptions.BadRequestException;
import task_tracker_api.api.factories.TaskDtoFactory;
import task_tracker_api.store.entities.TaskEntity;
import task_tracker_api.store.entities.TaskStateEntity;
import task_tracker_api.store.repositories.TaskRepository;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) //experimental
@Transactional
@RestController
public class TaskController {
    TaskRepository taskRepository;

    TaskDtoFactory taskDtoFactory;

    ControllerHelper controllerHelper;


    private static final String GET_TASKS = "/api/task-states/{task-state-id}";
    private static final String CREATE_TASK = "/api/task-states/{task-state-id}/task";
    private static final String DELETE_TASK= "/api/task/{task-id}";
    private static final String UPDATE_TASK = "/api/task/{task-id}";

    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable(name = "task-state-id") Long taskStateId) {

        TaskStateEntity taskStateEntity = controllerHelper.getTaskStateOrThrowException(taskStateId);

        return taskStateEntity.getTasks()
                .stream()
                .map(taskDtoFactory::makeTaskDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK)
    public TaskDto createTask(@PathVariable(name = "task-state-id") Long taskStateId,
                              @RequestParam(name = "task-name") String taskName,
                              @RequestParam(name = "task-description") String description){

        TaskStateEntity taskStateEntity = controllerHelper.getTaskStateOrThrowException(taskStateId);

        if(taskName.isBlank()){
            throw new BadRequestException("Task name can't be empty.");
        }
        if(description.isBlank()){
            throw new BadRequestException("Task description can't be empty.");
        }

        for(TaskEntity taskEntity : taskStateEntity.getTasks()){

            if(taskEntity.getName().equalsIgnoreCase(taskName)){
                throw new BadRequestException("Task name already exists.");
            }
        }

        TaskEntity taskEntity = TaskEntity.builder()
                    .name(taskName)
                    .description(description)
                    .build();

        taskStateEntity.getTasks().add(taskEntity);

        final TaskEntity savedTask = taskRepository.saveAndFlush(taskEntity);

        return taskDtoFactory.makeTaskDto(savedTask);
    }

    @PutMapping(UPDATE_TASK)
    public TaskDto updateTask(@PathVariable(name = "task-id") Long taskId,
                              @RequestParam(name = "task-description", required = false) String taskDescription){

        TaskEntity taskEntity = controllerHelper.getTaskOrThrowException(taskId);

        if(taskDescription.isBlank()){

            throw new BadRequestException("Task description can't be empty.");
        }
        if(taskEntity.getDescription().equalsIgnoreCase(taskDescription)){

            return taskDtoFactory.makeTaskDto(taskEntity);
        }else{

            taskEntity.setDescription(taskDescription);
        }

        final TaskEntity savedTask = taskRepository.saveAndFlush(taskEntity);

        return taskDtoFactory.makeTaskDto(savedTask);

    }

    @DeleteMapping(DELETE_TASK)
    public AckDto deleteTask(@PathVariable(name = "task-id") Long taskId){

        TaskEntity taskEntity = controllerHelper.getTaskOrThrowException(taskId);

        taskRepository.delete(taskEntity);

        return AckDto.builder().answer(true).build();

    }
}
