package com.example.demo.service;

import com.example.demo.entity.Asset;
import com.example.demo.entity.Audit;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.AuditRepository;
import com.example.demo.service.BarcodeScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;
    private final AssetRepository assetRepository;
    private final VisionAIService visionAIService;
    private final AssetService assetService;
    private final BarcodeScanService barcodeScanService;

    public AuditService(AuditRepository auditRepository,
                        AssetRepository assetRepository,
                        VisionAIService visionAIService,
                        AssetService assetService,
                        BarcodeScanService barcodeScanService) {
        this.auditRepository = auditRepository;
        this.assetRepository = assetRepository;
        this.visionAIService = visionAIService;
        this.assetService = assetService;
        this.barcodeScanService = barcodeScanService;
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

        Optional<Audit> auditOpt = auditRepository.findById(auditId);
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

    public Asset processAssetImage(MultipartFile imageFile) throws java.io.IOException {

        // Analyze image with Vision AI
        String aiAnalysis = visionAIService.analyzeAssetImage(imageFile);
        Map<String, Object> analysisResult = visionAIService.parseAnalysisResult(aiAnalysis);

        // Save image as evidence
        String imagePath = assetService.saveAssetImage(imageFile);

        // Save barcode scan record (image + AI analysis result)
        barcodeScanService.saveBarcodeScan(imageFile, aiAnalysis);

        // Create a minimal Asset object with data from analysis result
        Asset asset = new Asset();
        if (analysisResult != null) {
            String deviceNumber = (String) analysisResult.get("deviceNumber");
            if (deviceNumber != null) {
                asset.setDeviceNumber(deviceNumber);
            }
        }
        
        return asset;
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