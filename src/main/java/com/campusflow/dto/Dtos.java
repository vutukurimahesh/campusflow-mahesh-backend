package com.campusflow.dto;

import com.campusflow.entity.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** All request/response payloads, grouped to keep the project compact. */
public final class Dtos {
    private Dtos() {
    }

    // ---------- auth / user ----------
    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(min = 2, max = 100, message = "Name must be 2-100 characters") String name,
            @NotBlank(message = "Email is required") @Email(message = "Email is not valid") @Size(max = 150) String email,
            @NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required") @Email(message = "Email is not valid") String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record UserResponse(Long id, String name, String email, Instant createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getCreatedAt());
        }
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Name is required") @Size(min = 2, max = 100, message = "Name must be 2-100 characters") String name,
            String currentPassword,
            @Size(min = 8, max = 72, message = "New password must be 8-72 characters") String newPassword) {
    }

    // ---------- projects ----------
    public record ProjectRequest(
            @NotBlank(message = "Project name is required") @Size(max = 120, message = "Name is too long") String name,
            @Size(max = 2000, message = "Description is too long") String description,
            LocalDate deadline,
            ProjectStatus status) {
    }

    public record ProjectResponse(Long id, String name, String description, LocalDate deadline, ProjectStatus status,
                                  Long ownerId, String ownerName, MemberRole myRole, int memberCount,
                                  int totalTasks, int doneTasks, int overdueTasks, int progress,
                                  Instant createdAt, Instant updatedAt) {
    }

    // ---------- members ----------
    public record MemberRequest(
            @NotBlank(message = "Email is required") @Email(message = "Email is not valid") String email,
            @Size(max = 150, message = "Responsibility is too long") String responsibility) {
    }

    public record MemberUpdateRequest(
            @Size(max = 150, message = "Responsibility is too long") String responsibility) {
    }

    public record MemberResponse(Long id, Long userId, String name, String email, MemberRole role, String responsibility,
                                 Instant joinedAt, int assignedTasks, int completedTasks) {
    }

    public record TeamMemberResponse(Long userId, String name, String email, List<String> projects,
                                     List<String> responsibilities, int assignedTasks, int completedTasks, int openTasks) {
    }

    // ---------- tasks ----------
    public record TaskRequest(
            @NotBlank(message = "Title is required") @Size(max = 160, message = "Title is too long") String title,
            @Size(max = 2000, message = "Description is too long") String description,
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueDate,
            Long assigneeId) {
    }

    public record TaskStatusRequest(@NotNull(message = "Status is required") TaskStatus status) {
    }

    public record TaskResponse(Long id, Long projectId, String projectName, String title, String description,
                               TaskStatus status, TaskPriority priority, LocalDate dueDate,
                               Long assigneeId, String assigneeName, Long createdById, String createdByName,
                               Instant createdAt, Instant updatedAt, Instant completedAt, boolean overdue) {
    }

    // ---------- dashboard ----------
    public record DeadlineItem(String type, Long id, String title, Long projectId, String projectName,
                               LocalDate dueDate, String priority) {
    }

    public record ActivityItem(Long id, String message, String userName, Long projectId, String projectName, Instant createdAt) {
    }

    public record ProjectProgress(Long projectId, String name, int progress, int done, int total) {
    }

    public record DailyPoint(LocalDate date, String label, int completed, int created) {
    }

    public record DashboardResponse(int totalProjects, int activeProjects, int pendingTasks, int completedTasks,
                                    int overdueTasks, List<DeadlineItem> upcomingDeadlines,
                                    List<ActivityItem> recentActivity, List<ProjectProgress> projectProgress,
                                    List<DailyPoint> weeklyProductivity) {
    }

    // ---------- analytics ----------
    public record CountPoint(String name, int value) {
    }

    public record WorkloadPoint(Long userId, String name, int total, int done, int open) {
    }

    public record WeekPoint(String label, int completed, int created) {
    }

    public record AnalyticsResponse(int totalTasks, int completedTasks, int completionRate, int overdueTasks,
                                    Double avgCompletionDays, List<CountPoint> tasksByStatus,
                                    List<CountPoint> tasksByPriority, List<WorkloadPoint> workload,
                                    List<DailyPoint> last14Days, List<WeekPoint> weekly, List<ProjectProgress> projectProgress) {
    }
}
