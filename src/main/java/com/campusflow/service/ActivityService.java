package com.campusflow.service;

import com.campusflow.entity.Activity;
import com.campusflow.entity.Project;
import com.campusflow.entity.User;
import com.campusflow.repository.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final ActivityRepository activities;

    public ActivityService(ActivityRepository activities) {
        this.activities = activities;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(Project project, User user, String message) {
        Activity a = new Activity();
        a.setProject(project);
        a.setUser(user);
        a.setMessage(message.length() > 300 ? message.substring(0, 297) + "..." : message);
        activities.save(a);
    }
}
