package com.campusflow.controller;

import com.campusflow.dto.Dtos.*;
import com.campusflow.security.CurrentUser;
import com.campusflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping("/mine")
    public List<TaskResponse> mine() {
        return tasks.listMine(CurrentUser.id());
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return tasks.get(id, CurrentUser.id());
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest req) {
        return tasks.update(id, CurrentUser.id(), req);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest req) {
        return tasks.updateStatus(id, CurrentUser.id(), req.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tasks.delete(id, CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
