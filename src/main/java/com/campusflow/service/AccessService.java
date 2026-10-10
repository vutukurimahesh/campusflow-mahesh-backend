package com.campusflow.service;

import com.campusflow.entity.MemberRole;
import com.campusflow.entity.Project;
import com.campusflow.entity.ProjectMember;
import com.campusflow.exception.ApiException;
import com.campusflow.repository.ProjectMemberRepository;
import com.campusflow.repository.ProjectRepository;
import org.springframework.stereotype.Service;

/** Central place for "is this user allowed to touch this project" checks. */
@Service
public class AccessService {
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;

    public AccessService(ProjectRepository projects, ProjectMemberRepository members) {
        this.projects = projects;
        this.members = members;
    }

    /** Returns the project if the user is a member; otherwise 404 (hides existence from outsiders). */
    public Project requireMember(Long projectId, Long userId) {
        Project project = projects.findById(projectId).orElseThrow(() -> ApiException.notFound("Project not found"));
        if (!members.existsByProjectIdAndUserId(projectId, userId)) {
            throw ApiException.notFound("Project not found");
        }
        return project;
    }

    public Project requireOwner(Long projectId, Long userId) {
        Project project = requireMember(projectId, userId);
        ProjectMember pm = members.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> ApiException.notFound("Project not found"));
        if (pm.getRole() != MemberRole.OWNER) {
            throw ApiException.forbidden("Only the project owner can do this");
        }
        return project;
    }
}
