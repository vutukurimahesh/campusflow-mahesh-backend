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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MemberService {
    private final ProjectMemberRepository members;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final AccessService access;
    private final ActivityService activity;

    public MemberService(ProjectMemberRepository members, ProjectRepository projects, TaskRepository tasks,
                         UserRepository users, AccessService access, ActivityService activity) {
        this.members = members;
        this.projects = projects;
        this.tasks = tasks;
        this.users = users;
        this.access = access;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list(Long projectId, Long userId) {
        access.requireMember(projectId, userId);
        Map<Long, List<Task>> byAssignee = tasks.findByProjectId(projectId).stream()
                .filter(t -> t.getAssignee() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignee().getId()));
        return members.findByProjectId(projectId).stream().map(m -> {
            List<Task> ts = byAssignee.getOrDefault(m.getUser().getId(), List.of());
            int done = (int) ts.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            return new MemberResponse(m.getId(), m.getUser().getId(), m.getUser().getName(), m.getUser().getEmail(),
                    m.getRole(), m.getResponsibility(), m.getJoinedAt(), ts.size(), done);
        }).toList();
    }

    @Transactional
    public MemberResponse add(Long projectId, Long actorId, MemberRequest req) {
        Project project = access.requireOwner(projectId, actorId);
        User target = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> ApiException.notFound("No registered user found with that email"));
        if (members.existsByProjectIdAndUserId(projectId, target.getId())) {
            throw ApiException.conflict("This user is already a member of the project");
        }
        ProjectMember pm = new ProjectMember();
        pm.setProject(project);
        pm.setUser(target);
        pm.setRole(MemberRole.MEMBER);
        pm.setResponsibility(clean(req.responsibility()));
        pm = members.save(pm);
        activity.log(project, users.getReferenceById(actorId), target.getName() + " joined the project");
        return new MemberResponse(pm.getId(), target.getId(), target.getName(), target.getEmail(), pm.getRole(),
                pm.getResponsibility(), pm.getJoinedAt(), 0, 0);
    }

    @Transactional
    public MemberResponse updateResponsibility(Long projectId, Long memberId, Long actorId, MemberUpdateRequest req) {
        Project project = access.requireOwner(projectId, actorId);
        ProjectMember pm = findInProject(projectId, memberId);
        pm.setResponsibility(clean(req.responsibility()));
        members.save(pm);
        activity.log(project, users.getReferenceById(actorId),
                "Responsibility of " + pm.getUser().getName() + " was updated");
        return new MemberResponse(pm.getId(), pm.getUser().getId(), pm.getUser().getName(), pm.getUser().getEmail(),
                pm.getRole(), pm.getResponsibility(), pm.getJoinedAt(), 0, 0);
    }

    @Transactional
    public void remove(Long projectId, Long memberId, Long actorId) {
        Project project = access.requireOwner(projectId, actorId);
        ProjectMember pm = findInProject(projectId, memberId);
        if (pm.getRole() == MemberRole.OWNER) {
            throw ApiException.badRequest("The project owner cannot be removed");
        }
        // Unassign this user's tasks in the project.
        for (Task t : tasks.findByProjectId(projectId)) {
            if (t.getAssignee() != null && t.getAssignee().getId().equals(pm.getUser().getId())) {
                t.setAssignee(null);
                tasks.save(t);
            }
        }
        String name = pm.getUser().getName();
        members.delete(pm);
        activity.log(project, users.getReferenceById(actorId), name + " was removed from the project");
    }

    private ProjectMember findInProject(Long projectId, Long memberId) {
        ProjectMember pm = members.findById(memberId).orElseThrow(() -> ApiException.notFound("Member not found"));
        if (!pm.getProject().getId().equals(projectId)) {
            throw ApiException.notFound("Member not found");
        }
        return pm;
    }

    private String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
