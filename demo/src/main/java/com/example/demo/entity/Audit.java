package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Asset asset;

    @Column(nullable = false)
    private String auditType;

    private String auditorName;
    private String auditLocation;
    private Double auditLatitude;
    private Double auditLongitude;
    private String condition;

    @Lob
    @JdbcTypeCode(SqlTypes.CLOB)
    private String notes;
    private String status;
    private String evidenceImagePath;
    @Lob
    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "ai_analysis_result", columnDefinition = "LONGTEXT")
    private String aiAnalysisResult;
    private String deviceNumber;
    private String department;
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime auditDate;
    @Column
    private LocalDateTime completedAt;
}
