package com.example.demo.service;

import com.example.demo.entity.Asset;
import com.example.demo.repository.AssetRepository;
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
public class AssetService {

    private final AssetRepository assetRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<Asset> getAllAssets() {
        return assetRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Asset> getAssetById(Long id) {
        return assetRepository.findById(id);
    }

    public List<Asset> getAssetsByDepartment(String department) {
        return assetRepository.findByDepartment(department);
    }

    public byte[] getAssetImage(String filename) throws IOException {
        Path filePath = Paths.get(uploadDir, "assets", filename);
        return Files.readAllBytes(filePath);
    }

    public Asset getAssetByBarcode(String barcode) {
        return this.assetRepository.findByBarcode(barcode);
    }

    public String saveAssetImage(MultipartFile imageFile) throws IOException {
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

    public Asset createOrUpdateAsset(Map<String, Object> analysisResult,
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

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}