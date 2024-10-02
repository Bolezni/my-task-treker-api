package task_tracker_api.api.factories;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import task_tracker_api.api.dto.TaskStateDto;
import task_tracker_api.store.entities.TaskStateEntity;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@Component
public class TaskStateDtoFactory {

    TaskDtoFactory taskDtoFactory;

    public TaskStateDto makeTaskStateDto(TaskStateEntity entity){
        return TaskStateDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .leftTaskStateId(entity.getLeftTaskState()
                        .map(TaskStateEntity::getId)
                        .orElse(null))
                .rightTaskStateId(entity.getRightTaskState()
                        .map(TaskStateEntity::getId)
                        .orElse(null))
                .createAt(entity.getCreateAt())
                .tasks(entity.getTasks()
                        .stream()
                        .map(taskDtoFactory::makeTaskDto)
                        .collect(Collectors.toList()))
                .build();

    }
}
