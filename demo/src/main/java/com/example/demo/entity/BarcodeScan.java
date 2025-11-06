package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "barcode_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BarcodeScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String barcodeImg; // Đường dẫn file ảnh barcode

    @Lob
    @Column(name = "analysis_result", columnDefinition = "LONGTEXT")
    private String analysisResult; // Response từ AI

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime scannedAt; // Thời gian scan
}

