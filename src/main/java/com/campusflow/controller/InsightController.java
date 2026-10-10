package com.campusflow.controller;

import com.campusflow.dto.Dtos.*;
import com.campusflow.security.CurrentUser;
import com.campusflow.service.InsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InsightController {
    private final InsightService insights;

    public InsightController(InsightService insights) {
        this.insights = insights;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return insights.dashboard(CurrentUser.id());
    }

    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return insights.analytics(CurrentUser.id());
    }

    @GetMapping("/team")
    public List<TeamMemberResponse> team() {
        return insights.team(CurrentUser.id());
    }
}
