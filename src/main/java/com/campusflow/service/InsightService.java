package com.campusflow.service;

import com.campusflow.dto.Dtos.*;
import com.campusflow.entity.*;
import com.campusflow.repository.ActivityRepository;
import com.campusflow.repository.ProjectMemberRepository;
import com.campusflow.repository.ProjectRepository;
import com.campusflow.repository.TaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/** Dashboard, analytics and team views. Every number is computed from persisted rows. */
@Service
public class InsightService {
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final TaskRepository tasks;
    private final ActivityRepository activities;
    private final TimeService time;

    public InsightService(ProjectRepository projects, ProjectMemberRepository members, TaskRepository tasks,
                          ActivityRepository activities, TimeService time) {
        this.projects = projects;
        this.members = members;
        this.tasks = tasks;
        this.activities = activities;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long userId) {
        LocalDate today = time.today();
        List<Project> mine = projects.findAllForUser(userId);
        List<Long> ids = mine.stream().map(Project::getId).toList();
        List<Task> all = ids.isEmpty() ? List.of() : tasks.findByProjectIds(ids);

        int active = (int) mine.stream().filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();
        int pending = (int) all.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();
        int completed = (int) all.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        int overdue = (int) all.stream().filter(t -> TaskService.isOverdue(t, today)).count();

        // Upcoming deadlines: open tasks and unfinished projects due within the next 14 days (or already overdue tasks excluded).
        LocalDate horizon = today.plusDays(14);
        List<DeadlineItem> deadlines = new ArrayList<>();
        for (Task t : all) {
            if (t.getStatus() != TaskStatus.DONE && t.getDueDate() != null
                    && !t.getDueDate().isBefore(today) && !t.getDueDate().isAfter(horizon)) {
                deadlines.add(new DeadlineItem("TASK", t.getId(), t.getTitle(), t.getProject().getId(),
                        t.getProject().getName(), t.getDueDate(), t.getPriority().name()));
            }
        }
        for (Project p : mine) {
            if (p.getStatus() != ProjectStatus.COMPLETED && p.getDeadline() != null
                    && !p.getDeadline().isBefore(today) && !p.getDeadline().isAfter(horizon)) {
                deadlines.add(new DeadlineItem("PROJECT", p.getId(), p.getName(), p.getId(), p.getName(),
                        p.getDeadline(), null));
            }
        }
        deadlines.sort(Comparator.comparing(DeadlineItem::dueDate));
        if (deadlines.size() > 8) {
            deadlines = new ArrayList<>(deadlines.subList(0, 8));
        }

        List<ActivityItem> recent = ids.isEmpty() ? List.of() :
                activities.findRecent(ids, PageRequest.of(0, 10)).stream()
                        .map(a -> new ActivityItem(a.getId(), a.getMessage(),
                                a.getUser() == null ? "Someone" : a.getUser().getName(),
                                a.getProject().getId(), a.getProject().getName(), a.getCreatedAt()))
                        .toList();

        return new DashboardResponse(mine.size(), active, pending, completed, overdue, deadlines, recent,
                progressList(mine, all), lastDays(all, 7, today));
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(Long userId) {
        LocalDate today = time.today();
        List<Project> mine = projects.findAllForUser(userId);
        List<Long> ids = mine.stream().map(Project::getId).toList();
        List<Task> all = ids.isEmpty() ? List.of() : tasks.findByProjectIds(ids);

        int total = all.size();
        int done = (int) all.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        int overdue = (int) all.stream().filter(t -> TaskService.isOverdue(t, today)).count();
        int rate = total == 0 ? 0 : Math.round(done * 100f / total);

        OptionalDouble avg = all.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE && t.getCompletedAt() != null)
                .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getCompletedAt()).toMinutes() / 1440.0)
                .average();
        Double avgDays = avg.isPresent() ? Math.round(avg.getAsDouble() * 10.0) / 10.0 : null;

        List<CountPoint> byStatus = new ArrayList<>();
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.add(new CountPoint(s.name(), (int) all.stream().filter(t -> t.getStatus() == s).count()));
        }
        List<CountPoint> byPriority = new ArrayList<>();
        for (TaskPriority p : TaskPriority.values()) {
            byPriority.add(new CountPoint(p.name(), (int) all.stream().filter(t -> t.getPriority() == p).count()));
        }

        // Workload per member (tasks assigned to them within my projects).
        Map<Long, WorkloadPoint> workload = new LinkedHashMap<>();
        Map<Long, List<Task>> byAssignee = all.stream().filter(t -> t.getAssignee() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignee().getId(), LinkedHashMap::new, Collectors.toList()));
        byAssignee.forEach((uid, ts) -> {
            int d = (int) ts.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            workload.put(uid, new WorkloadPoint(uid, ts.get(0).getAssignee().getName(), ts.size(), d, ts.size() - d));
        });
        List<WorkloadPoint> workloadList = workload.values().stream()
                .sorted(Comparator.comparingInt(WorkloadPoint::total).reversed()).toList();

        // Weekly series: last 8 weeks, weeks start on Monday.
        LocalDate thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<WeekPoint> weekly = new ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            LocalDate start = thisWeek.minusWeeks(i);
            LocalDate end = start.plusDays(6);
            int c = (int) all.stream().filter(t -> t.getCompletedAt() != null
                    && inRange(t.getCompletedAt().atZone(time.zone()).toLocalDate(), start, end)).count();
            int cr = (int) all.stream().filter(t -> inRange(t.getCreatedAt().atZone(time.zone()).toLocalDate(), start, end)).count();
            weekly.add(new WeekPoint(start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + start.getDayOfMonth(), c, cr));
        }

        return new AnalyticsResponse(total, done, rate, overdue, avgDays, byStatus, byPriority, workloadList,
                lastDays(all, 14, today), weekly, progressList(mine, all));
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> team(Long userId) {
        List<Long> ids = projects.findAllForUser(userId).stream().map(Project::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<ProjectMember> all = members.findByProjectIds(ids);
        Map<Long, List<Task>> byAssignee = tasks.findByProjectIds(ids).stream().filter(t -> t.getAssignee() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignee().getId()));

        Map<Long, List<ProjectMember>> byUser = all.stream()
                .collect(Collectors.groupingBy(m -> m.getUser().getId(), LinkedHashMap::new, Collectors.toList()));
        List<TeamMemberResponse> out = new ArrayList<>();
        byUser.forEach((uid, ms) -> {
            User u = ms.get(0).getUser();
            List<Task> ts = byAssignee.getOrDefault(uid, List.of());
            int d = (int) ts.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            out.add(new TeamMemberResponse(uid, u.getName(), u.getEmail(),
                    ms.stream().map(m -> m.getProject().getName()).distinct().toList(),
                    ms.stream().map(ProjectMember::getResponsibility).filter(Objects::nonNull).distinct().toList(),
                    ts.size(), d, ts.size() - d));
        });
        out.sort(Comparator.comparing(TeamMemberResponse::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private List<ProjectProgress> progressList(List<Project> mine, List<Task> all) {
        Map<Long, List<Task>> byProject = all.stream().collect(Collectors.groupingBy(t -> t.getProject().getId()));
        return mine.stream().map(p -> {
            List<Task> ts = byProject.getOrDefault(p.getId(), List.of());
            int d = (int) ts.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            int pct = ts.isEmpty() ? 0 : Math.round(d * 100f / ts.size());
            return new ProjectProgress(p.getId(), p.getName(), pct, d, ts.size());
        }).toList();
    }

    private List<DailyPoint> lastDays(List<Task> all, int days, LocalDate today) {
        List<DailyPoint> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            int c = (int) all.stream().filter(t -> t.getCompletedAt() != null
                    && t.getCompletedAt().atZone(time.zone()).toLocalDate().equals(day)).count();
            int cr = (int) all.stream().filter(t -> t.getCreatedAt().atZone(time.zone()).toLocalDate().equals(day)).count();
            out.add(new DailyPoint(day, day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), c, cr));
        }
        return out;
    }

    private boolean inRange(LocalDate d, LocalDate start, LocalDate end) {
        return !d.isBefore(start) && !d.isAfter(end);
    }
}
