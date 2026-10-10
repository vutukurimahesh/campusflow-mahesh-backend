package com.campusflow.repository;

import com.campusflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("select p from Project p join fetch p.owner where p.id in " +
            "(select pm.project.id from ProjectMember pm where pm.user.id = :userId) " +
            "order by p.updatedAt desc")
    List<Project> findAllForUser(@Param("userId") Long userId);
}
