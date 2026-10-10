package com.campusflow.repository;

import com.campusflow.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("select t from Task t join fetch t.project left join fetch t.assignee join fetch t.createdBy " +
            "where t.project.id = :projectId order by t.createdAt desc")
    List<Task> findByProjectId(@Param("projectId") Long projectId);

    @Query("select t from Task t join fetch t.project left join fetch t.assignee join fetch t.createdBy " +
            "where t.project.id in :projectIds")
    List<Task> findByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    @Query("select t from Task t join fetch t.project left join fetch t.assignee join fetch t.createdBy " +
            "where t.id = :id")
    Optional<Task> findDetailed(@Param("id") Long id);

    @Query("select t from Task t join fetch t.project left join fetch t.assignee join fetch t.createdBy " +
            "where t.assignee.id = :userId order by t.dueDate asc nulls last, t.createdAt desc")
    List<Task> findByAssigneeId(@Param("userId") Long userId);
}
