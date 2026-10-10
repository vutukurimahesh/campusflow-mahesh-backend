package com.campusflow.controller;

import com.campusflow.dto.Dtos.*;
import com.campusflow.entity.ProjectStatus;
import com.campusflow.entity.TaskPriority;
import com.campusflow.entity.TaskStatus;
import com.campusflow.security.CurrentUser;
import com.campusflow.service.MemberService;
import com.campusflow.service.ProjectService;
import com.campusflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projects;
    private final MemberService members;
    private final TaskService tasks;

    public ProjectController(ProjectService projects, MemberService members, TaskService tasks) {
        this.projects = projects;
        this.members = members;
        this.tasks = tasks;
    }

    // ----- projects -----
    @GetMapping
    public List<ProjectResponse> list(@RequestParam(required = false) String search,
                                      @RequestParam(required = false) ProjectStatus status) {
        return projects.list(CurrentUser.id(), search, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest req) {
        return projects.create(CurrentUser.id(), req);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id) {
        return projects.get(id, CurrentUser.id());
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        return projects.update(id, CurrentUser.id(), req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projects.delete(id, CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    // ----- members -----
    @GetMapping("/{id}/members")
    public List<MemberResponse> members(@PathVariable Long id) {
        return members.list(id, CurrentUser.id());
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@PathVariable Long id, @Valid @RequestBody MemberRequest req) {
        return members.add(id, CurrentUser.id(), req);
    }

    @PutMapping("/{id}/members/{memberId}")
    public MemberResponse updateMember(@PathVariable Long id, @PathVariable Long memberId,
                                       @Valid @RequestBody MemberUpdateRequest req) {
        return members.updateResponsibility(id, memberId, CurrentUser.id(), req);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        members.remove(id, memberId, CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    // ----- tasks of a project -----
    @GetMapping("/{id}/tasks")
    public List<TaskResponse> tasks(@PathVariable Long id,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) TaskStatus status,
                                    @RequestParam(required = false) TaskPriority priority,
                                    @RequestParam(required = false) Long assigneeId) {
        return tasks.listForProject(id, CurrentUser.id(), search, status, priority, assigneeId);
    }

    @PostMapping("/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@PathVariable Long id, @Valid @RequestBody TaskRequest req) {
        return tasks.create(id, CurrentUser.id(), req);
    }
}
