package com.niyotechnologies.claimlens.coverage.repository;

import com.niyotechnologies.claimlens.coverage.entity.CoverageAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoverageAnswerRepository extends JpaRepository<CoverageAnswer, Long> {
}
