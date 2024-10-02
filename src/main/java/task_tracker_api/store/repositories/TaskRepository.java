package task_tracker_api.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import task_tracker_api.store.entities.TaskEntity;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
}
