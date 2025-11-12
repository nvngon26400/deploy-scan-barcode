package com.example.demo.repository;

import com.example.demo.entity.Audit;
import com.example.demo.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Long> {

    List<Audit> findByAuditorName(String auditorName);

    List<Audit> findAllByOrderByAuditDateDesc();
}
