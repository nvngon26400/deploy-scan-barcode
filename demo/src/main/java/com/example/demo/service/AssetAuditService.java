package com.example.demo.service;

import com.example.demo.entity.Asset;
import com.example.demo.entity.Audit;
import com.example.demo.repository.AssetRepository;
import com.example.demo.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
/**
 * @deprecated Use AssetService and AuditService instead.
 */
@Deprecated
public class AssetAuditService {

    private final AssetRepository assetRepository;
    private final AuditRepository auditRepository;
    private final VisionAIService visionAIService;
    private final ImageService imageService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public AssetAuditService(AssetRepository assetRepository,
                             AuditRepository auditRepository,
                             VisionAIService visionAIService,
                             ImageService imageService) {
        this.assetRepository = assetRepository;
        this.auditRepository = auditRepository;
        this.visionAIService = visionAIService;
        this.imageService = imageService;
    }

    public Asset processAssetImage(MultipartFile imageFile) throws IOException {

        // Analyze image with Vision AI
        String aiAnalysis = visionAIService.analyzeAssetImage(imageFile);
        Map<String, Object> analysisResult = visionAIService.parseAnalysisResult(aiAnalysis);

        // Save image as evidence
        String imagePath = saveAssetImage(imageFile);

        // Create or update asset
        Asset asset = new Asset();

        return asset;
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

    public List<Asset> getAllAssets() {
        return assetRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Audit> getAllAudits() {
        return auditRepository.findAllByOrderByAuditDateDesc();
    }

    public Optional<Asset> getAssetById(Long id) {
        return assetRepository.findById(id);
    }

    public Optional<Audit> getAuditById(Long id) {
        return auditRepository.findById(id);
    }

    public List<Asset> getAssetsByDepartment(String department) {
        return assetRepository.findByDepartment(department);
    }

    public List<Audit> getAuditsByAuditor(String auditorName) {
        return auditRepository.findByAuditorName(auditorName);
    }

    private Asset createOrUpdateAsset(Map<String, Object> analysisResult,
                                      String imagePath,
                                      Double latitude,
                                      Double longitude) {

        String deviceNumber = (String) analysisResult.get("deviceNumber");
        Optional<Asset> existingAsset = assetRepository.findByDeviceNumber(deviceNumber);

        Asset asset;
        if (existingAsset.isPresent()) {
            asset = existingAsset.get();
            // Update existing asset
            asset.setImagePath(imagePath);
            asset.setLatitude(latitude);
            asset.setLongitude(longitude);
            asset.setAiExtractedInfo(analysisResult.toString());
        } else {
            // Create new asset
            asset = new Asset();
            asset.setDeviceNumber(deviceNumber);
            asset.setDeviceName((String) analysisResult.getOrDefault("model", "Unknown Device"));
            asset.setDepartment((String) analysisResult.getOrDefault("department", "Unknown"));
            asset.setLocation((String) analysisResult.getOrDefault("location", "Unknown"));
            asset.setLatitude(latitude);
            asset.setLongitude(longitude);
            asset.setBarcode((String) analysisResult.getOrDefault("barcode", ""));
            asset.setSerialNumber((String) analysisResult.getOrDefault("serialNumber", ""));
            asset.setModel((String) analysisResult.getOrDefault("model", ""));
            asset.setManufacturer((String) analysisResult.getOrDefault("manufacturer", ""));
            asset.setStatus("Pending Audit");
            asset.setAiExtractedInfo(analysisResult.toString());
            asset.setImagePath(imagePath);
            asset.setCreatedAt(LocalDateTime.now());
        }

        return assetRepository.save(asset);
    }

    private void createInitialAudit(Asset asset,
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

    private String saveAssetImage(MultipartFile imageFile) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir + "assets/");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFileName = imageFile.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String storedFileName = "ASSET_" + UUID.randomUUID().toString() + fileExtension;

        // Save file to disk
        Path filePath = uploadPath.resolve(storedFileName);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public byte[] getAssetImage(String filename) throws IOException {
        Path filePath = Paths.get(uploadDir, "assets", filename);
        return Files.readAllBytes(filePath);
    }

    public Asset getAssetByBarcode(String barcode) {
        return this.assetRepository.findByBarcode(barcode);
    }
}
