package com.example.demo.repository;

import com.example.demo.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByStoredFileName(String storedFileName);

    List<Image> findByOriginalFileNameContainingIgnoreCase(String fileName);

    List<Image> findByContentType(String contentType);

    List<Image> findAllByOrderByUploadedAtDesc();
}