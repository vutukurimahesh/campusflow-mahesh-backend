package com.campusflow.service;

import com.campusflow.dto.Dtos.*;
import com.campusflow.entity.*;
import com.campusflow.exception.ApiException;
import com.campusflow.repository.ProjectMemberRepository;
import com.campusflow.repository.TaskRepository;
import com.campusflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectMemberRepository members;
    private final UserRepository users;
    private final AccessService access;
    private final ActivityService activity;
    private final TimeService time;

    public TaskService(TaskRepository tasks, ProjectMemberRepository members, UserRepository users,
                       AccessService access, ActivityService activity, TimeService time) {
        this.tasks = tasks;
        this.members = members;
        this.users = users;
        this.access = access;
        this.activity = activity;
        this.time = time;
    }

    public static boolean isOverdue(Task t, LocalDate today) {
        return t.getDueDate() != null && t.getDueDate().isBefore(today) && t.getStatus() != TaskStatus.DONE;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listForProject(Long projectId, Long userId, String search, TaskStatus status,
                                             TaskPriority priority, Long assigneeId) {
        access.requireMember(projectId, userId);
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return tasks.findByProjectId(projectId).stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> assigneeId == null || (t.getAssignee() != null && t.getAssignee().getId().equals(assigneeId)))
                .filter(t -> q.isEmpty() || t.getTitle().toLowerCase(Locale.ROOT).contains(q)
                        || (t.getDescription() != null && t.getDescription().toLowerCase(Locale.ROOT).contains(q)))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listMine(Long userId) {
        return tasks.findByAssigneeId(userId).stream()
                .filter(t -> members.existsByProjectIdAndUserId(t.getProject().getId(), userId))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long taskId, Long userId) {
        Task t = load(taskId);
        access.requireMember(t.getProject().getId(), userId);
        return toResponse(t);
    }

    @Transactional
    public TaskResponse create(Long projectId, Long userId, TaskRequest req) {
        Project project = access.requireMember(projectId, userId);
        User actor = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Task t = new Task();
        t.setProject(project);
        t.setCreatedBy(actor);
        applyFields(t, req, projectId);
        t = tasks.save(t);
        activity.log(project, actor, actor.getName() + " created task \"" + t.getTitle() + "\"");
        return toResponse(load(t.getId()));
    }

    @Transactional
    public TaskResponse update(Long taskId, Long userId, TaskRequest req) {
        Task t = load(taskId);
        Project project = access.requireMember(t.getProject().getId(), userId);
        User actor = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        applyFields(t, req, project.getId());
        tasks.save(t);
        activity.log(project, actor, actor.getName() + " updated task \"" + t.getTitle() + "\"");
        return toResponse(load(taskId));
    }

    @Transactional
    public TaskResponse updateStatus(Long taskId, Long userId, TaskStatus status) {
        Task t = load(taskId);
        Project project = access.requireMember(t.getProject().getId(), userId);
        User actor = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        TaskStatus old = t.getStatus();
        if (old != status) {
            setStatus(t, status);
            tasks.save(t);
            activity.log(project, actor, actor.getName() + " moved \"" + t.getTitle() + "\" to " + label(status));
        }
        return toResponse(load(taskId));
    }

    @Transactional
    public void delete(Long taskId, Long userId) {
        Task t = load(taskId);
        Project project = access.requireMember(t.getProject().getId(), userId);
        boolean isOwner = project.getOwner().getId().equals(userId);
        if (!isOwner && !t.getCreatedBy().getId().equals(userId)) {
            throw ApiException.forbidden("Only the project owner or the task creator can delete this task");
        }
        User actor = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        activity.log(project, actor, actor.getName() + " deleted task \"" + t.getTitle() + "\"");
        tasks.delete(t);
    }

    private Task load(Long taskId) {
        return tasks.findDetailed(taskId).orElseThrow(() -> ApiException.notFound("Task not found"));
    }

    private void applyFields(Task t, TaskRequest req, Long projectId) {
        t.setTitle(req.title().trim());
        t.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        t.setPriority(req.priority() == null ? TaskPriority.MEDIUM : req.priority());
        t.setDueDate(req.dueDate());
        setStatus(t, req.status() == null ? TaskStatus.TODO : req.status());
        if (req.assigneeId() == null) {
            t.setAssignee(null);
        } else {
            if (!members.existsByProjectIdAndUserId(projectId, req.assigneeId())) {
                throw ApiException.badRequest("Tasks can only be assigned to project members");
            }
            t.setAssignee(users.getReferenceById(req.assigneeId()));
        }
    }

    private void setStatus(Task t, TaskStatus status) {
        if (status == TaskStatus.DONE && t.getStatus() != TaskStatus.DONE) {
            t.setCompletedAt(Instant.now());
        } else if (status != TaskStatus.DONE) {
            t.setCompletedAt(null);
        }
        t.setStatus(status);
    }

    private String label(TaskStatus s) {
        return switch (s) {
            case TODO -> "To Do";
            case IN_PROGRESS -> "In Progress";
            case DONE -> "Done";
        };
    }

    TaskResponse toResponse(Task t) {
        User a = t.getAssignee();
        return new TaskResponse(t.getId(), t.getProject().getId(), t.getProject().getName(), t.getTitle(),
                t.getDescription(), t.getStatus(), t.getPriority(), t.getDueDate(),
                a == null ? null : a.getId(), a == null ? null : a.getName(),
                t.getCreatedBy().getId(), t.getCreatedBy().getName(),
                t.getCreatedAt(), t.getUpdatedAt(), t.getCompletedAt(), isOverdue(t, time.today()));
    }
}
