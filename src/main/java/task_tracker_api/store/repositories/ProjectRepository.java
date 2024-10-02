package task_tracker_api.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import task_tracker_api.store.entities.ProjectEntity;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ProjectRepository extends JpaRepository<ProjectEntity,Long> {

    Optional<ProjectEntity> findByName(String name);

    Stream<ProjectEntity> streamAllByNameStartsWithIgnoreCase(String prefixName);

    Stream<ProjectEntity> streamAllBy();
}
