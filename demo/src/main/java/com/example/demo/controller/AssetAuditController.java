package com.example.demo.controller;

import com.example.demo.entity.Asset;
import com.example.demo.entity.Audit;
import com.example.demo.service.AssetService;
import com.example.demo.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

@Controller
@Slf4j
public class AssetAuditController {

    private final AssetService assetService;
    private final AuditService auditService;

    public AssetAuditController(AssetService assetService, AuditService auditService) {
        this.assetService = assetService;
        this.auditService = auditService;
    }

    @GetMapping("/audit")
    public String auditDashboard(Model model) {
        List<Asset> assets = assetService.getAllAssets();
        List<Audit> audits = auditService.getAllAudits();

        model.addAttribute("assets", assets);
        model.addAttribute("audits", audits);

        return "audit-dashboard";
    }

    @GetMapping("/audit/capture")
    public String captureAssetPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "capture-asset";
    }

    @PostMapping("/audit/capture")
    public String processAssetCapture(@RequestParam("imageFile") MultipartFile imageFile,
                                      @RequestParam("auditorName") String auditorName,
                                      @RequestParam(value = "latitude", required = false) Double latitude,
                                      @RequestParam(value = "longitude", required = false) Double longitude,
                                      Model model) throws UnsupportedEncodingException {
        try {
            Asset asset = auditService.processAssetImage(imageFile);

            model.addAttribute("asset", asset);
            model.addAttribute("message", "Asset captured successfully! Device Number: " + asset.getDeviceNumber());

            return "audit-complete";

        } catch (IOException e) {
            return "redirect:/audit/capture?error=" + java.net.URLEncoder.encode("Failed to process image: " + e.getMessage(), "UTF-8");
        } catch (Exception e) {
            return "redirect:/audit/capture?error=" + java.net.URLEncoder.encode("Error processing asset: " + e.getMessage(), "UTF-8");
        }
    }

    @GetMapping("/audit/complete/{auditId}")
    public String completeAuditPage(@PathVariable Long auditId, Model model) {
        Optional<Audit> auditOpt = auditService.getAuditById(auditId);
        if (auditOpt.isEmpty()) {
            model.addAttribute("error", "Audit not found");
            return "error";
        }

        model.addAttribute("audit", auditOpt.get());
        return "complete-audit";
    }

    @PostMapping("/audit/complete/{auditId}")
    public String completeAudit(@PathVariable Long auditId,
                                @RequestParam("condition") String condition,
                                @RequestParam("notes") String notes,
                                @RequestParam("status") String status,
                                Model model) {
        try {
            Audit audit = auditService.completeAudit(auditId, condition, notes, status);
            model.addAttribute("audit", audit);
            model.addAttribute("message", "Audit completed successfully!");

            return "audit-complete";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to complete audit: " + e.getMessage());
            return "complete-audit";
        }
    }

    @GetMapping("/audit/assets")
    public String assetsList(Model model) {
        List<Asset> assets = assetService.getAllAssets();
        model.addAttribute("assets", assets);
        return "assets-list";
    }

    @GetMapping("/audit/audits")
    public String auditsList(Model model) {
        List<Audit> audits = auditService.getAllAudits();
        model.addAttribute("audits", audits);
        return "audits-list";
    }

    @GetMapping("/audit/asset/{id}")
    public String assetDetail(@PathVariable Long id, Model model) {
        Optional<Asset> assetOpt = assetService.getAssetById(id);
        if (assetOpt.isEmpty()) {
            model.addAttribute("error", "Asset not found");
            return "error";
        }

        model.addAttribute("asset", assetOpt.get());
        return "asset-detail";
    }

}
 