package com.example.demo.repository;

import com.example.demo.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByDeviceNumber(String deviceNumber);

    List<Asset> findByDepartment(String department);

    List<Asset> findByStatus(String status);

    Asset findByBarcode(String barcode);

    List<Asset> findBySerialNumber(String serialNumber);

    List<Asset> findAllByOrderByCreatedAtDesc();
}
