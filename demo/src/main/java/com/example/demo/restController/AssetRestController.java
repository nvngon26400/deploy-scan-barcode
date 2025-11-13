package com.example.demo.restController;

import com.example.demo.entity.Asset;
import com.example.demo.service.AssetService;
import com.example.demo.service.AuditService;
import com.example.demo.service.BarcodeScanService;
import com.example.demo.service.BarcodeDecoderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.zxing.Result;

@RestController
@RequestMapping("/api")
@Slf4j
public class AssetRestController {

    private final AssetService assetService;
    private final AuditService auditService;
    private final BarcodeScanService barcodeScanService;
    private final BarcodeDecoderService barcodeDecoderService;
    private final com.example.demo.service.VisionAIService visionAIService;
    private final com.example.demo.service.GoogleCloudVisionService googleCloudVisionService;

    public AssetRestController(AssetService assetService, 
                               AuditService auditService,
                               BarcodeScanService barcodeScanService,
                               BarcodeDecoderService barcodeDecoderService,
                               com.example.demo.service.VisionAIService visionAIService,
                               com.example.demo.service.GoogleCloudVisionService googleCloudVisionService) {
        this.assetService = assetService;
        this.auditService = auditService;
        this.barcodeScanService = barcodeScanService;
        this.barcodeDecoderService = barcodeDecoderService;
        this.visionAIService = visionAIService;
        this.googleCloudVisionService = googleCloudVisionService;
    }

    @GetMapping("/assets")
    public ResponseEntity<List<Asset>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    // Audit APIs đã được chuyển sang AuditRestController

    @GetMapping("/assets/{barcode}")
    public ResponseEntity<Asset> getAssetByBarcode(@PathVariable String barcode) {
        Asset asset = assetService.getAssetByBarcode(barcode);
        return ResponseEntity.ok(asset);
    }

    @GetMapping("/assets/department/{department}")
    public ResponseEntity<List<Asset>> getAssetsByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(assetService.getAssetsByDepartment(department));
    }

    @GetMapping("/images/assets/{filename:.+}")
    public ResponseEntity<byte[]> getAssetImage(@PathVariable String filename) {
        try {
            byte[] imageBytes = assetService.getAssetImage(filename);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/assets/capture")
    public ResponseEntity<Map<String, Object>> captureAsset(
            @RequestParam("imageFile") MultipartFile imageFile) {

        try {
            Asset asset = auditService.processAssetImage(imageFile);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asset captured and barcode scan saved successfully!");
            response.put("assetId", asset.getId() != null ? asset.getId() : null);
            response.put("deviceNumber", asset.getDeviceNumber() != null ? asset.getDeviceNumber() : "N/A");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to capture asset: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/barcode-scans/export")
    public ResponseEntity<byte[]> exportBarcodeScansToExcel() {
        try {
            byte[] excelBytes = barcodeScanService.exportToExcel();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("barcode-scans-export.xlsx")
                    .build());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
                    
        } catch (Exception e) {
//            log.error("Failed to export barcode scans to Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/barcode/decode")
    public ResponseEntity<Map<String, Object>> decodeBarcode(
            @RequestParam("imageFile") MultipartFile imageFile) {
        Map<String, Object> response = new HashMap<>();
        if (imageFile == null || imageFile.isEmpty()) {
            response.put("success", false);
            response.put("message", "Vui lòng upload ảnh hợp lệ.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<Map<String, String>> barcodes = new java.util.ArrayList<>();
            Set<String> seenTexts = new HashSet<>();
            String lastMethod = "unknown";

            // Bước 1: Thử decode bằng thuật toán xử lý ảnh nâng cao (với deblurring, denoising, morphological ops)
            log.info("Bước 1: Thử decode bằng thuật toán xử lý ảnh nâng cao...");
            try {
                List<Result> results = barcodeDecoderService.decode(imageFile.getBytes());
                
                for (Result r : results) {
                    String text = r.getText();
                    if (seenTexts.add(text)) {
                        Map<String, String> item = new HashMap<>();
                        item.put("text", text);
                        item.put("format", r.getBarcodeFormat().toString());
                        item.put("method", "advanced_algorithm");
                        barcodes.add(item);
                        lastMethod = "advanced_algorithm";
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi khi decode bằng thuật toán: " + e.getMessage());
            }

            // Bước 2: Nếu không tìm thấy, thử Google Cloud Vision API (rất mạnh cho ảnh mờ/chói)
            if (barcodes.isEmpty()) {
                log.info("Bước 2: Thử Google Cloud Vision API...");
                try {
                    List<Map<String, String>> googleBarcodes = googleCloudVisionService.detectBarcode(imageFile);
                    for (Map<String, String> googleBarcode : googleBarcodes) {
                        String text = googleBarcode.get("text");
                        if (text != null && !text.isEmpty() && seenTexts.add(text)) {
                            Map<String, String> item = new HashMap<>();
                            item.put("text", text);
                            item.put("format", googleBarcode.getOrDefault("format", "GOOGLE_VISION"));
                            item.put("method", "google_vision");
                            barcodes.add(item);
                            lastMethod = "google_vision";
                        }
                    }
                } catch (Exception googleException) {
                    log.warn("Lỗi khi sử dụng Google Cloud Vision: " + googleException.getMessage());
                }
            }

            // Bước 3: Nếu vẫn không tìm thấy, thử GPT-4 Vision AI
            if (barcodes.isEmpty()) {
                log.info("Bước 3: Thử GPT-4 Vision AI...");
                try {
                    List<Map<String, String>> aiBarcodes = visionAIService.decodeBarcodeWithAI(imageFile);
                    for (Map<String, String> aiBarcode : aiBarcodes) {
                        String text = aiBarcode.get("text");
                        if (text != null && !text.isEmpty() && seenTexts.add(text)) {
                            Map<String, String> item = new HashMap<>();
                            item.put("text", text);
                            item.put("format", aiBarcode.getOrDefault("format", "AI_DETECTED"));
                            item.put("method", "gpt4_vision");
                            barcodes.add(item);
                            lastMethod = "gpt4_vision";
                        }
                    }
                } catch (Exception aiException) {
                    log.warn("Lỗi khi sử dụng GPT-4 Vision AI: " + aiException.getMessage());
                }
            }

            boolean success = !barcodes.isEmpty();
            response.put("success", success);
            response.put("count", barcodes.size());
            response.put("barcodes", barcodes);
            
            if (success) {
                String methodMessage;
                switch (lastMethod) {
                    case "advanced_algorithm":
                        methodMessage = "Phát hiện mã thành công bằng thuật toán xử lý ảnh nâng cao (deblurring, denoising, morphological operations).";
                        break;
                    case "google_vision":
                        methodMessage = "Phát hiện mã thành công bằng Google Cloud Vision API (rất mạnh cho ảnh mờ/chói).";
                        break;
                    case "gpt4_vision":
                        methodMessage = "Phát hiện mã thành công bằng GPT-4 Vision AI.";
                        break;
                    default:
                        methodMessage = "Phát hiện mã thành công.";
                        break;
                }
                response.put("message", methodMessage);
            } else {
                response.put("message", "Không phát hiện được mã từ ảnh. " +
                    "Đã thử: (1) Thuật toán xử lý ảnh nâng cao (deblurring, denoising, morphological), " +
                    "(2) Google Cloud Vision API, (3) GPT-4 Vision AI. " +
                    "Hãy thử chụp lại với góc nhìn khác, tránh lóa/chói quá mức.");
            }
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi giải mã: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}