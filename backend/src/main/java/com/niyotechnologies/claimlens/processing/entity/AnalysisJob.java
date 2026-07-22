package com.niyotechnologies.claimlens.processing.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_job")
public class AnalysisJob extends ProcessingJob {
}
