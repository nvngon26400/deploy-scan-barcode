package com.example.demo.controller;

import com.example.demo.entity.Image;
import com.example.demo.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("")
public class ImageController {
 
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Image> images = imageService.getAllImages();
        model.addAttribute("images", images);
        return "index";
    }
    
    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }
    
    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "description", required = false) String description,
                             Model model) {
        try {
            Image uploadedImage = imageService.uploadImage(file, description);
            model.addAttribute("message", "File uploaded successfully: " + uploadedImage.getOriginalFileName());
            model.addAttribute("imageId", uploadedImage.getId());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (IOException e) {
            model.addAttribute("error", "Failed to upload file: " + e.getMessage());
        }
        return "upload";
    }
    
    @GetMapping("/images")
    public ResponseEntity<List<Image>> getAllImages() {
        List<Image> images = imageService.getAllImages();
        return ResponseEntity.ok(images);
    }
    
    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        try {
            Optional<Image> imageOpt = imageService.getImageById(id);
            if (imageOpt.isPresent()) {
                Image image = imageOpt.get();
                byte[] imageBytes = imageService.getImageBytes(id);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(image.getContentType()));
                headers.setContentLength(imageBytes.length);
                headers.set("Content-Disposition", "inline; filename=\"" + image.getOriginalFileName() + "\"");
                
                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
//            log.error("Error reading image file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/images/{id}/download")
    public ResponseEntity<byte[]> downloadImage(@PathVariable Long id) {
        try {
            Optional<Image> imageOpt = imageService.getImageById(id);
            if (imageOpt.isPresent()) {
                Image image = imageOpt.get();
                byte[] imageBytes = imageService.getImageBytes(id);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentLength(imageBytes.length);
                headers.set("Content-Disposition", "attachment; filename=\"" + image.getOriginalFileName() + "\"");
                
                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
//            log.error("Error downloading image file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/images/search")
    public ResponseEntity<List<Image>> searchImages(@RequestParam String fileName) {
        List<Image> images = imageService.searchImagesByFileName(fileName);
        return ResponseEntity.ok(images);
    }
    
    @DeleteMapping("/images/{id}")
    public ResponseEntity<String> deleteImage(@PathVariable Long id) {
        try {
            imageService.deleteImage(id);
            return ResponseEntity.ok("Image deleted successfully");
        } catch (IOException e) {
//            log.error("Error deleting image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete image: " + e.getMessage());
        }
    }
    
    @GetMapping("/images/info/{id}")
    public ResponseEntity<Image> getImageInfo(@PathVariable Long id) {
        Optional<Image> imageOpt = imageService.getImageById(id);
        return imageOpt.map(image -> ResponseEntity.ok(image))
                      .orElse(ResponseEntity.notFound().build());
    }
}
