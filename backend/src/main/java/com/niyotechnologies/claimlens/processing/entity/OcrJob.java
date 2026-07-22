package com.niyotechnologies.claimlens.processing.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ocr_job")
public class OcrJob extends ProcessingJob {
}
