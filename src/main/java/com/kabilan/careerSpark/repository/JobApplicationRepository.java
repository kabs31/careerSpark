package com.kabilan.careerSpark.repository;


import com.kabilan.careerSpark.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByResumeId(Long resumeId);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.status = 'COMPLETED' ORDER BY ja.submittedAt DESC")
    List<JobApplication> findRecentCompletedApplications();
}
