package task_tracker_api.api.factories;

import org.springframework.stereotype.Component;
import task_tracker_api.api.dto.TaskDto;
import task_tracker_api.api.dto.TaskStateDto;
import task_tracker_api.store.entities.TaskEntity;
import task_tracker_api.store.entities.TaskStateEntity;

@Component
public class TaskDtoFactory {

    public TaskDto makeTaskDto(TaskEntity entity){
        return TaskDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createAt(entity.getCreateAt())
                .description(entity.getDescription())
                .build();

    }
}
