package com.example.demo.restController;

import com.example.demo.dto.AssetDTO;
import com.example.demo.dto.AuditRequest;
import com.example.demo.entity.Asset;
import com.example.demo.entity.Audit;
import com.example.demo.mapper.AssetMapper;
import com.example.demo.service.AssetService;
import com.example.demo.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit")
public class AuditRestController {

    private final AssetMapper assetMapper;
    private final AssetService assetService;
    private final AuditService auditService;

    public AuditRestController(AssetMapper assetMapper,
                               AssetService assetService,
                               AuditService auditService) {
        this.assetMapper = assetMapper;
        this.assetService = assetService;
        this.auditService = auditService;
    }

    @PostMapping("/{barcode}")
    public AssetDTO getAssetByBarcode(@PathVariable String barcode) {
        Asset asset = this.assetService.getAssetByBarcode(barcode);
        return assetMapper.toDTO(asset);
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<Audit>> getAllAudits() {
        return ResponseEntity.ok(auditService.getAllAudits());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Audit> getAudit(@PathVariable Long id) {
        Optional<Audit> audit = auditService.getAuditById(id);
        return audit.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/auditor/{auditorName}")
    public ResponseEntity<List<Audit>> getAuditsByAuditor(@PathVariable String auditorName) {
        return ResponseEntity.ok(auditService.getAuditsByAuditor(auditorName));
    }
    
    @PostMapping("/{auditId}/complete")
    public ResponseEntity<Map<String, Object>> completeAuditApi(@PathVariable Long auditId,
                                                            @RequestParam("condition") String condition,
                                                            @RequestParam("notes") String notes,
                                                            @RequestParam("status") String status) {
        try {
            Audit audit = auditService.completeAudit(auditId, condition, notes, status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Audit completed successfully!");
            response.put("auditId", audit.getId());
            response.put("status", audit.getStatus());
            response.put("completedAt", audit.getCompletedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to complete audit: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/test")
    public ResponseEntity<String> testApi(@RequestBody AuditRequest request) {
        return ResponseEntity.ok(request.getBarcode());
    }
}