package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
@Data
@EntityListeners(AuditingEntityListener.class)
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String deviceNumber;

    @Column(nullable = false)
    private String deviceName;
    private String department;
    private String location;
    private Double latitude;
    private Double longitude;
    private String barcode;
    private String serialNumber;
    private String model;
    private String manufacturer;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String aiExtractedInfo;
    private String imagePath;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime lastAudited;
}
