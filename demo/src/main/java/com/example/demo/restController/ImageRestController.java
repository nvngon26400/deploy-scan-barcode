package com.example.demo.restController;

import com.example.demo.entity.Image;
import com.example.demo.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/images")
@Slf4j
public class ImageRestController {

    private final ImageService imageService;

    public ImageRestController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<List<Image>> getAllImages() {
        List<Image> images = imageService.getAllImages();
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{id}")
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/download")
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "description", required = false) String description) {
        try {
            Image uploadedImage = imageService.uploadImage(file, description);
            return ResponseEntity.ok().body(uploadedImage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }
}