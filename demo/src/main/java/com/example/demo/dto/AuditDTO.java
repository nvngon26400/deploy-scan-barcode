package com.example.demo.dto;

import com.example.demo.entity.Asset;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditDTO {
    private Long id;
    private Asset asset;
    private String auditType;
    private String auditorName;
    private String auditLocation;
    private Double auditLatitude;
    private Double auditLongitude;
    private String condition;
    private String notes;
    private String status;
    private String evidenceImagePath;
    private String aiAnalysisResult;
    private String deviceNumber;
    private String department;
    private LocalDateTime auditDate;
    private LocalDateTime completedAt;
}