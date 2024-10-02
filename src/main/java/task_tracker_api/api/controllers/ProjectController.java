package task_tracker_api.api.controllers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import task_tracker_api.api.controllers.helpers.ControllerHelper;
import task_tracker_api.api.dto.AckDto;
import task_tracker_api.api.dto.ProjectDto;
import task_tracker_api.api.exceptions.BadRequestException;
import task_tracker_api.api.factories.ProjectDtoFactory;
import task_tracker_api.store.entities.ProjectEntity;
import task_tracker_api.store.repositories.ProjectRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) //experimental
@Transactional
@RestController
public class ProjectController {

    ProjectRepository projectRepository;

    ProjectDtoFactory projectDtoFactory;

    ControllerHelper controllerHelper;

    private static final String FETCH_PROJECT = "/api/projects";
    private static final String DELETE_PROJECT = "/api/projects/{id}";
    private static final String CREATE_OR_UPDATE_PROJECT = "/api/projects";

    @GetMapping(FETCH_PROJECT)
    public List<ProjectDto> fetchProjects(
            @RequestParam(value = "prefix_name",required = false)
            Optional<String> optionalPrefixName) {

        optionalPrefixName = optionalPrefixName.filter(prefix -> !prefix.trim().isEmpty());

        Stream<ProjectEntity> projectStream = optionalPrefixName
                .map(projectRepository::streamAllByNameStartsWithIgnoreCase)
                .orElseGet(projectRepository::streamAllBy);

        return projectStream.map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }


    @PutMapping(CREATE_OR_UPDATE_PROJECT)
    public ProjectDto createOrUpdateProject(
            @RequestParam(value = "id", required = false) Optional<Long> optionalProjectId,
            @RequestParam(value = "name", required = false) Optional<String> optionalProjectName){

        optionalProjectName = optionalProjectName.filter(projectName -> !projectName.trim().isEmpty());

        boolean isCreate = optionalProjectId.isEmpty();

        final ProjectEntity project = optionalProjectId
                .map(controllerHelper::getProjectOrThrowException)
                .orElseGet(() -> ProjectEntity.builder().build());

        if(isCreate && optionalProjectName.isEmpty()){
            throw new BadRequestException("Project name cannot be empty");
        }

        optionalProjectName.ifPresent(projectName ->{
                projectRepository
                        .findByName(projectName)
                        .filter(anotherProject -> !Objects.equals(anotherProject.getId(), project.getId()))
                        .ifPresent(anotherProject -> {
                            throw new BadRequestException(String.format("Project \"%s\" already exist", projectName)
                            );
                        });
                project.setName(projectName);
        });

        final ProjectEntity newProject = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(newProject);
    }



    @DeleteMapping(DELETE_PROJECT)
    public AckDto deleteProject(@PathVariable(value = "id") Long id) {

        controllerHelper.getProjectOrThrowException(id);

        projectRepository.deleteById(id);

        return AckDto.makeDefault(true);
    }
}
