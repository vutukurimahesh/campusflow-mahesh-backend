package com.campusflow.repository;

import com.campusflow.entity.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query("select a from Activity a join fetch a.project left join fetch a.user " +
            "where a.project.id in :projectIds order by a.createdAt desc")
    List<Activity> findRecent(@Param("projectIds") Collection<Long> projectIds, Pageable pageable);
}
