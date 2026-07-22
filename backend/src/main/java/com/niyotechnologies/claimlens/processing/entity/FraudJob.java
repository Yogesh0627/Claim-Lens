package com.niyotechnologies.claimlens.processing.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "fraud_job")
public class FraudJob extends ProcessingJob {
}
