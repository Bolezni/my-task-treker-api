package task_tracker_api.api.controllers.helpers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import task_tracker_api.api.exceptions.NotFoundException;
import task_tracker_api.store.entities.ProjectEntity;
import task_tracker_api.store.entities.TaskEntity;
import task_tracker_api.store.entities.TaskStateEntity;
import task_tracker_api.store.repositories.ProjectRepository;
import task_tracker_api.store.repositories.TaskRepository;
import task_tracker_api.store.repositories.TaskStateRepository;

import java.util.Optional;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) //experimental
@Component
@Transactional
public class ControllerHelper {

    ProjectRepository projectRepository;

    TaskStateRepository taskStateRepository;

    TaskRepository taskRepository;

    public ProjectEntity getProjectOrThrowException(Long id) {
        return projectRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Project with \"%s\" doesn't exist", id)));
    }

    public void replaceOldTaskStatePosition(TaskStateEntity changeTaskState){
        Optional<TaskStateEntity> optionalOldLeftTaskState = changeTaskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalOldRightTaskState = changeTaskState.getRightTaskState();

        optionalOldLeftTaskState.ifPresent(anotherTaskState -> {
            anotherTaskState.setRightTaskState(optionalOldRightTaskState.orElse(null));

            taskStateRepository.saveAndFlush(anotherTaskState);
        });

        optionalOldRightTaskState.ifPresent(anotherTaskState -> {
            anotherTaskState.setLeftTaskState(optionalOldLeftTaskState.orElse(null));

            taskStateRepository.saveAndFlush(anotherTaskState);
        });

    }

    public TaskStateEntity getTaskStateOrThrowException(Long taskStateId){
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "Task state with \"%s\" id doesn't exist.", taskStateId
                )
                ));
    }

    public TaskEntity getTaskOrThrowException(Long taskId){
        return taskRepository
                .findById(taskId)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "Task  with \"%s\" id doesn't exist.", taskId
                        )
                ));
    }
}
