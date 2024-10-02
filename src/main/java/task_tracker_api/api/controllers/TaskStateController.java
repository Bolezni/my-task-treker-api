package task_tracker_api.api.controllers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import task_tracker_api.api.controllers.helpers.ControllerHelper;
import task_tracker_api.api.dto.AckDto;
import task_tracker_api.api.dto.TaskStateDto;
import task_tracker_api.api.exceptions.BadRequestException;
import task_tracker_api.api.factories.TaskStateDtoFactory;
import task_tracker_api.store.entities.ProjectEntity;
import task_tracker_api.store.entities.TaskStateEntity;
import task_tracker_api.store.repositories.TaskStateRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) //experimental
@Transactional
@RestController
public class TaskStateController {
    TaskStateRepository taskStateRepository;

    TaskStateDtoFactory taskStateDtoFactory;

    ControllerHelper controllerHelper;

    private static final String GET_TASK_STATES = "/api/projects/{id}/task-states";
    private static final String CREATE_TASK_STATES = "/api/projects/{id}/task-states";
    private static final String DELETE_TASK_STATE = "/api/task-states/{task-state-id}";
    private static final String UPDATE_TASK_STATE = "/api/task-states/{task-state-id}";
    public static final String CHANGE_TASK_STATE_POSITION = "/api/task-states/{task-state-id}/position/change";

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable(name = "id") Long id) {

        ProjectEntity project = controllerHelper.getProjectOrThrowException(id);

        return project.getTaskStates()
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK_STATES)
    public TaskStateDto createTaskState(@PathVariable(name = "id") Long id,
                                        @RequestParam(name = "task_state_name") String taskStateName) {

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task state name can't be empty.");
        }

        ProjectEntity project = controllerHelper.getProjectOrThrowException(id);

        Optional<TaskStateEntity> optionalAnotherTaskState = Optional.empty();

        for (TaskStateEntity taskState : project.getTaskStates()) {

            if (taskState.getName().equalsIgnoreCase(taskStateName)) {
                throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
            }

            if (taskState.getRightTaskState().isEmpty()) {
                optionalAnotherTaskState = Optional.of(taskState);
                break;
            }
        }

        TaskStateEntity taskState = taskStateRepository.saveAndFlush(
                TaskStateEntity.builder()
                        .name(taskStateName)
                        .project(project)
                        .build());

        optionalAnotherTaskState.ifPresent(anotherTaskState -> {

            taskState.setLeftTaskState(anotherTaskState);

            anotherTaskState.setRightTaskState(taskState);

            taskStateRepository.saveAndFlush(anotherTaskState);
        });

        final TaskStateEntity savedTaskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(savedTaskState);
    }

    @PutMapping(UPDATE_TASK_STATE)
    public TaskStateDto updateTaskState(@PathVariable(name = "task-state-id") Long taskStateId,
                                        @RequestParam(name = "task-state-name") String taskStateName) {

        if (taskStateName.isBlank()) {
            throw new BadRequestException("Task state name can't be empty.");
        }

        TaskStateEntity taskState =controllerHelper.getTaskStateOrThrowException(taskStateId);

        taskStateRepository.findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(
                taskState.getProject().getId(),
                        taskState.getName())
                .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId))
                .ifPresent(anotherTaskState -> {
                    throw new BadRequestException(String.format("Task state \"%s\" already exists.", taskStateName));
                });

        taskState.setName(taskStateName);

        taskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    @PutMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskState(@PathVariable(name = "task-state-id") Long taskStateId,
                                        @RequestParam(name = "left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId){

        TaskStateEntity changeTaskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        ProjectEntity project = changeTaskState.getProject();

        Optional<Long> optionalOldLeftTaskStateId = changeTaskState
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        if (optionalOldLeftTaskStateId.equals(optionalLeftTaskStateId)) {
            return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
        }

        Optional<TaskStateEntity> optionalNewLeftTaskState = optionalLeftTaskStateId
                .map(leftTaskStateId -> {

                    if (taskStateId.equals(leftTaskStateId)) {
                        throw new BadRequestException("Left task state id equals changed task state.");
                    }

                    TaskStateEntity leftTaskStateEntity = controllerHelper.getTaskStateOrThrowException(leftTaskStateId);

                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) {
                        throw new BadRequestException("Task state position can be changed within the same project.");
                    }

                    return leftTaskStateEntity;
                });

        Optional<TaskStateEntity> optionalNewRightTaskState;
        if (optionalNewLeftTaskState.isEmpty()) {

            optionalNewRightTaskState = project
                    .getTaskStates()
                    .stream()
                    .filter(anotherTaskState -> anotherTaskState.getLeftTaskState().isEmpty())
                    .findAny();
        } else {

            optionalNewRightTaskState = optionalNewLeftTaskState
                    .get()
                    .getRightTaskState();
        }

        controllerHelper.replaceOldTaskStatePosition(changeTaskState);

        if (optionalNewLeftTaskState.isPresent()) {

            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();

            newLeftTaskState.setRightTaskState(changeTaskState);

            changeTaskState.setLeftTaskState(newLeftTaskState);
        } else {
            changeTaskState.setLeftTaskState(null);
        }

        if (optionalNewRightTaskState.isPresent()) {

            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();

            newRightTaskState.setLeftTaskState(changeTaskState);

            changeTaskState.setRightTaskState(newRightTaskState);
        } else {
            changeTaskState.setRightTaskState(null);
        }

        changeTaskState = taskStateRepository.saveAndFlush(changeTaskState);

        optionalNewLeftTaskState
                .ifPresent(taskStateRepository::saveAndFlush);

        optionalNewRightTaskState
                .ifPresent(taskStateRepository::saveAndFlush);

        return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
    }

    @DeleteMapping(DELETE_TASK_STATE)
    public AckDto deleteTaskState(@PathVariable(name = "task-state-id") Long taskStateId){

        TaskStateEntity taskState = controllerHelper.getTaskStateOrThrowException(taskStateId);

        controllerHelper.replaceOldTaskStatePosition(taskState);

        taskStateRepository.delete(taskState);

        return AckDto.builder().answer(true).build();
    }
}
