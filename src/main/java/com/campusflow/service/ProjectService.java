package com.campusflow.service;

import com.campusflow.dto.Dtos.*;
import com.campusflow.entity.*;
import com.campusflow.exception.ApiException;
import com.campusflow.repository.ProjectMemberRepository;
import com.campusflow.repository.ProjectRepository;
import com.campusflow.repository.TaskRepository;
import com.campusflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final AccessService access;
    private final ActivityService activity;
    private final TimeService time;

    public ProjectService(ProjectRepository projects, ProjectMemberRepository members, TaskRepository tasks,
                          UserRepository users, AccessService access, ActivityService activity, TimeService time) {
        this.projects = projects;
        this.members = members;
        this.tasks = tasks;
        this.users = users;
        this.access = access;
        this.activity = activity;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(Long userId, String search, ProjectStatus status) {
        List<Project> mine = projects.findAllForUser(userId);
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<Project> filtered = mine.stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> q.isEmpty() || p.getName().toLowerCase(Locale.ROOT).contains(q)
                        || (p.getDescription() != null && p.getDescription().toLowerCase(Locale.ROOT).contains(q)))
                .toList();
        return toResponses(filtered, userId);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long projectId, Long userId) {
        Project p = access.requireMember(projectId, userId);
        return toResponses(List.of(p), userId).get(0);
    }

    @Transactional
    public ProjectResponse create(Long userId, ProjectRequest req) {
        User owner = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Project p = new Project();
        apply(p, req);
        p.setOwner(owner);
        p = projects.save(p);

        ProjectMember pm = new ProjectMember();
        pm.setProject(p);
        pm.setUser(owner);
        pm.setRole(MemberRole.OWNER);
        pm.setResponsibility("Project owner");
        members.save(pm);

        activity.log(p, owner, owner.getName() + " created the project \"" + p.getName() + "\"");
        return toResponses(List.of(p), userId).get(0);
    }

    @Transactional
    public ProjectResponse update(Long projectId, Long userId, ProjectRequest req) {
        Project p = access.requireOwner(projectId, userId);
        apply(p, req);
        p = projects.save(p);
        User actor = users.getReferenceById(userId);
        activity.log(p, actor, "Project \"" + p.getName() + "\" was updated");
        return toResponses(List.of(p), userId).get(0);
    }

    @Transactional
    public void delete(Long projectId, Long userId) {
        Project p = access.requireOwner(projectId, userId);
        projects.delete(p);
    }

    private void apply(Project p, ProjectRequest req) {
        p.setName(req.name().trim());
        p.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        p.setDeadline(req.deadline());
        p.setStatus(req.status() == null ? ProjectStatus.ACTIVE : req.status());
    }

    /** Builds responses with member counts and task statistics computed from persisted rows. */
    List<ProjectResponse> toResponses(List<Project> list, Long userId) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> ids = list.stream().map(Project::getId).toList();
        Map<Long, List<ProjectMember>> memberMap = members.findByProjectIds(ids).stream()
                .collect(Collectors.groupingBy(m -> m.getProject().getId()));
        Map<Long, List<Task>> taskMap = tasks.findByProjectIds(ids).stream()
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));
        LocalDate today = time.today();

        List<ProjectResponse> out = new ArrayList<>();
        for (Project p : list) {
            List<ProjectMember> ms = memberMap.getOrDefault(p.getId(), List.of());
            List<Task> ts = taskMap.getOrDefault(p.getId(), List.of());
            int total = ts.size();
            int done = (int) ts.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            int overdue = (int) ts.stream().filter(t -> TaskService.isOverdue(t, today)).count();
            int progress = total == 0 ? 0 : Math.round(done * 100f / total);
            MemberRole myRole = ms.stream().filter(m -> m.getUser().getId().equals(userId))
                    .map(ProjectMember::getRole).findFirst().orElse(MemberRole.MEMBER);
            out.add(new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getDeadline(), p.getStatus(),
                    p.getOwner().getId(), p.getOwner().getName(), myRole, ms.size(), total, done, overdue, progress,
                    p.getCreatedAt(), p.getUpdatedAt()));
        }
        return out;
    }
}
