package com.example.demo.service;

import com.example.demo.entity.Image;
import com.example.demo.repository.ImageRepository;
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
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image uploadImage(MultipartFile file, String description) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate image type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String storedFileName = UUID.randomUUID().toString() + fileExtension;

        // Save file to disk
        Path filePath = uploadPath.resolve(storedFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Save image info to database
        Image image = new Image();
        image.setOriginalFileName(originalFileName);
        image.setStoredFileName(storedFileName);
        image.setFilePath(filePath.toString());
        image.setFileSize(file.getSize());
        image.setContentType(contentType);
        image.setDescription(description);
        image.setUploadedAt(LocalDateTime.now());

        return imageRepository.save(image);
    }

    public Optional<Image> getImageById(Long id) {
        return imageRepository.findById(id);
    }

    public Optional<Image> getImageByStoredFileName(String storedFileName) {
        return imageRepository.findByStoredFileName(storedFileName);
    }

    public List<Image> getAllImages() {
        return imageRepository.findAllByOrderByUploadedAtDesc();
    }

    public List<Image> searchImagesByFileName(String fileName) {
        return imageRepository.findByOriginalFileNameContainingIgnoreCase(fileName);
    }

    public List<Image> getImagesByContentType(String contentType) {
        return imageRepository.findByContentType(contentType);
    }

    public void deleteImage(Long id) throws IOException {
        Optional<Image> imageOpt = imageRepository.findById(id);
        if (imageOpt.isPresent()) {
            Image image = imageOpt.get();

            // Delete file from disk
            Path filePath = Paths.get(image.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            // Delete from database
            imageRepository.delete(image);
        }
    }

    public byte[] getImageBytes(Long id) throws IOException {
        Optional<Image> imageOpt = imageRepository.findById(id);
        if (imageOpt.isPresent()) {
            Image image = imageOpt.get();
            Path filePath = Paths.get(image.getFilePath());
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
        }
        throw new IOException("Image file not found");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
