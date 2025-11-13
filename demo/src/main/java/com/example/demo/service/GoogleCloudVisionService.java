package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service tích hợp Google Cloud Vision API cho barcode detection
 * Google Cloud Vision có khả năng detect barcode rất mạnh, kể cả ảnh mờ/chói
 */
@Service
@Slf4j
public class GoogleCloudVisionService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;

    public GoogleCloudVisionService(RestTemplate restTemplate,
                                    @Value("${google.vision.api.url:https://vision.googleapis.com/v1/images:annotate}") String apiUrl,
                                    @Value("${google.vision.api.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    /**
     * Detect barcode từ ảnh sử dụng Google Cloud Vision API
     */
    public List<Map<String, String>> detectBarcode(MultipartFile imageFile) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Google Cloud Vision API key not configured");
            return new ArrayList<>();
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
            
            Map<String, Object> request = createVisionRequest(base64Image);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Google Cloud Vision API sử dụng key parameter hoặc OAuth
            String url = apiUrl + "?key=" + apiKey;
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            
            return parseBarcodeResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("Error calling Google Cloud Vision API", e);
            return new ArrayList<>();
        }
    }

    private Map<String, Object> createVisionRequest(String base64Image) {
        Map<String, Object> request = new HashMap<>();
        
        // Request features - yêu cầu detect barcode
        Map<String, Object> feature = new HashMap<>();
        feature.put("type", "BARCODE_DETECTION");
        feature.put("maxResults", 10);
        
        // Image content
        Map<String, Object> image = new HashMap<>();
        image.put("content", base64Image);
        
        // Request item
        Map<String, Object> requestItem = new HashMap<>();
        requestItem.put("image", image);
        requestItem.put("features", new Object[]{feature});
        
        // Image context - có thể thêm hints
        Map<String, Object> imageContext = new HashMap<>();
        // Có thể thêm language hints nếu cần
        
        requestItem.put("imageContext", imageContext);
        
        request.put("requests", new Object[]{requestItem});
        
        return request;
    }

    private List<Map<String, String>> parseBarcodeResponse(String responseBody) {
        List<Map<String, String>> barcodes = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode responses = root.get("responses");
            
            if (responses != null && responses.isArray() && responses.size() > 0) {
                JsonNode response = responses.get(0);
                JsonNode barcodeAnnotations = response.get("barcodeAnnotations");
                
                if (barcodeAnnotations != null && barcodeAnnotations.isArray()) {
                    for (JsonNode barcode : barcodeAnnotations) {
                        Map<String, String> barcodeInfo = new HashMap<>();
                        
                        // Lấy raw value
                        if (barcode.has("rawValue")) {
                            barcodeInfo.put("text", barcode.get("rawValue").asText());
                        }
                        
                        // Lấy format
                        if (barcode.has("format")) {
                            barcodeInfo.put("format", barcode.get("format").asText());
                        } else {
                            barcodeInfo.put("format", "GOOGLE_VISION");
                        }
                        
                        // Lấy value type nếu có
                        if (barcode.has("valueType")) {
                            barcodeInfo.put("valueType", barcode.get("valueType").asText());
                        }
                        
                        if (barcodeInfo.containsKey("text")) {
                            barcodes.add(barcodeInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Google Cloud Vision response", e);
        }
        
        return barcodes;
    }
}

