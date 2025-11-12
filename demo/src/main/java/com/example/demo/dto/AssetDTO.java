package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssetDTO {
    private Long id;
    private String barcode;
    private String deviceNumber;
    private String deviceName;
    private String department;
    private String location;
    private Double latitude;
    private Double longitude;
    private String serialNumber;
    private String model;
    private String manufacturer;
    private String status;
    private String aiExtractedInfo;
    private String imagePath;
    private LocalDateTime createdAt;
    private LocalDateTime lastAudited;
}