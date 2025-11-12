package com.example.demo.repository;

import com.example.demo.entity.BarcodeScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BarcodeScanRepository extends JpaRepository<BarcodeScan, Long> {

    List<BarcodeScan> findAllByOrderByScannedAtDesc();
}

