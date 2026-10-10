package com.campusflow.repository;

import com.campusflow.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    @Query("select pm from ProjectMember pm join fetch pm.user where pm.project.id = :projectId order by pm.joinedAt")
    List<ProjectMember> findByProjectId(@Param("projectId") Long projectId);

    @Query("select pm from ProjectMember pm join fetch pm.user join fetch pm.project where pm.project.id in :projectIds")
    List<ProjectMember> findByProjectIds(@Param("projectIds") Collection<Long> projectIds);
}
