package com.example.demo.service;

import com.example.demo.entity.Asset;
import com.example.demo.entity.Audit;
import com.example.demo.exception.BarcodeNotDetectException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.AuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;
    private final AssetRepository assetRepository;
    private final VisionAIService visionAIService;
    private final AssetService assetService;

    public AuditService(AuditRepository auditRepository,
                        AssetRepository assetRepository,
                        VisionAIService visionAIService,
                        AssetService assetService) {
        this.auditRepository = auditRepository;
        this.assetRepository = assetRepository;
        this.visionAIService = visionAIService;
        this.assetService = assetService;
    }

    public List<Audit> getAllAudits() {
        return auditRepository.findAllByOrderByAuditDateDesc();
    }

    public Optional<Audit> getAuditById(Long id) {
        return auditRepository.findById(id);
    }

    public List<Audit> getAuditsByAuditor(String auditorName) {
        return auditRepository.findByAuditorName(auditorName);
    }

    public Audit completeAudit(Long auditId,
                               String condition,
                               String notes,
                               String status) {

        Optional<Audit> auditOpt = getAuditById(auditId);
        if (auditOpt.isEmpty()) {
            throw new IllegalArgumentException("Audit not found");
        }

        Audit audit = auditOpt.get();
        audit.setCondition(condition);
        audit.setNotes(notes);
        audit.setStatus(status);
        audit.setCompletedAt(LocalDateTime.now());

        // Update asset status
        Asset asset = audit.getAsset();
        asset.setStatus(status);
        asset.setLastAudited(LocalDateTime.now());
        assetRepository.save(asset);

        return auditRepository.save(audit);
    }

    public void createInitialAudit(Asset asset,
                                    String auditorName,
                                    String aiAnalysis,
                                    String imagePath,
                                    Double latitude,
                                    Double longitude) {

        Audit audit = new Audit();
        audit.setAsset(asset);
        audit.setAuditType("Initial Assessment");
        audit.setAuditorName(auditorName);
        audit.setAuditLocation(asset.getLocation());
        audit.setAuditLatitude(latitude);
        audit.setAuditLongitude(longitude);
        audit.setStatus("In Progress");
        audit.setEvidenceImagePath(imagePath);
        audit.setAiAnalysisResult(aiAnalysis);
        audit.setDeviceNumber(asset.getDeviceNumber());
        audit.setDepartment(asset.getDepartment());
        audit.setAuditDate(LocalDateTime.now());

        auditRepository.save(audit);
    }
}