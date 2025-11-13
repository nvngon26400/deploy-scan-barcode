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

@Service
@Slf4j
public class VisionAIService {

    private final String apiUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public VisionAIService(RestTemplate restTemplate,
                           @Value("${vision.ai.api.url}") String apiUrl,
                           @Value("${vision.ai.api.key:}")  String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    public String analyzeAssetImage(MultipartFile imageFile) throws IOException {
        if (apiKey.isEmpty()) {
            log.warn("Vision AI API key not configured, returning mock data");
            return generateMockAnalysis();
        }

        try {
            // Encode image to base64
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());

            // Prepare the request payload
            Map<String, Object> requestBody = createVisionRequest(base64Image);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            // Parse response
            return parseVisionResponse(response.getBody());

        } catch (Exception e) {
            log.error("Error calling Vision AI API", e);
            return generateMockAnalysis();
        }
    }

    private Map<String, Object> createVisionRequest(String base64Image) {
        // Create a request map with the model set to GPT-4 Vision
        Map<String, Object> request = new HashMap<>(Map.of("model", "gpt-4o"));

        // Create a message map with the role set to "user"
        Map<String, Object> message = new HashMap<>(Map.of("role", "user"));

        // Create a content map for the text prompt
        Map<String, Object> content = new HashMap<>();
        content.put("type", "text"); // Specify the content type as text
        content.put("text", "Please read the text directly below the barcode in this image and return only that plain text"); // Instruction for the model

        // Create a content map for the image input
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url"); // Specify the content type as image

        // Create a map for the image URL using base64-encoded image data
        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image); // Embed the base64 image as a data URL

        // Add the image URL map to the image content
        imageContent.put("image_url", imageUrl);

        // Add both text and image content to the message
        message.put("content", new Object[]{content, imageContent});

        // Add the message to the request
        request.put("messages", new Object[]{message});

        // Set the maximum number of tokens for the response
        request.put("max_tokens", 1000);

        return request;
    }

    private String parseVisionResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Vision AI response", e);
        }
        return generateMockAnalysis();
    }

    private String generateMockAnalysis() {
        return """
                """;
    }

    public Map<String, Object> parseAnalysisResult(String analysisResult) {
        try {
            return objectMapper.readValue(analysisResult, Map.class);
        } catch (Exception e) {
            log.error("Error parsing analysis result", e);
            Map<String, Object> mockResult = new HashMap<>();
            mockResult.put("deviceNumber", "ASSET-2025-00001");
            mockResult.put("department", "Unknown");
            mockResult.put("condition", "Unknown");
            return mockResult;
        }
    }

    /**
     * Sử dụng AI Vision để decode barcode từ ảnh lóa sáng
     * @param imageFile Ảnh chứa barcode
     * @return Danh sách các barcode được phát hiện, mỗi item chứa text và format
     */
    public List<Map<String, String>> decodeBarcodeWithAI(MultipartFile imageFile) throws IOException {
        if (apiKey.isEmpty()) {
            log.warn("Vision AI API key not configured, cannot use AI barcode decoding");
            return new ArrayList<>();
        }

        try {
            // Encode image to base64
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());

            // Prepare the request payload với prompt chuyên biệt cho barcode
            Map<String, Object> requestBody = createBarcodeDecodeRequest(base64Image);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            // Parse response và trả về danh sách barcode
            return parseBarcodeResponse(response.getBody());

        } catch (Exception e) {
            log.error("Error calling Vision AI API for barcode decoding", e);
            return new ArrayList<>();
        }
    }

    private Map<String, Object> createBarcodeDecodeRequest(String base64Image) {
        Map<String, Object> request = new HashMap<>(Map.of("model", "gpt-4o"));

        Map<String, Object> message = new HashMap<>(Map.of("role", "user"));

        // Prompt chuyên biệt cho việc đọc barcode, kể cả khi ảnh bị lóa sáng
        Map<String, Object> content = new HashMap<>();
        content.put("type", "text");
        content.put("text", "Analyze this image and extract all barcode values you can detect. " +
                "Even if the image has glare or is overexposed, try to read the barcode. " +
                "Return the result as a JSON array of objects, each with 'text' (the barcode value) and 'format' (the barcode format like CODE_128, QR_CODE, etc.). " +
                "If you cannot detect any barcode, return an empty array []. " +
                "Example format: [{\"text\": \"1234567890\", \"format\": \"CODE_128\"}]");

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");

        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);

        imageContent.put("image_url", imageUrl);

        message.put("content", new Object[]{content, imageContent});

        request.put("messages", new Object[]{message});
        request.put("max_tokens", 1000);

        return request;
    }

    private List<Map<String, String>> parseBarcodeResponse(String responseBody) {
        List<Map<String, String>> barcodes = new ArrayList<>();
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    String content = message.get("content").asText();
                    
                    // Thử parse JSON từ content
                    // Content có thể là JSON array hoặc text mô tả
                    content = content.trim();
                    if (content.startsWith("[") && content.endsWith("]")) {
                        // Parse JSON array
                        JsonNode barcodeArray = objectMapper.readTree(content);
                        if (barcodeArray.isArray()) {
                            for (JsonNode barcodeNode : barcodeArray) {
                                Map<String, String> barcode = new HashMap<>();
                                if (barcodeNode.has("text")) {
                                    barcode.put("text", barcodeNode.get("text").asText());
                                }
                                if (barcodeNode.has("format")) {
                                    barcode.put("format", barcodeNode.get("format").asText());
                                } else {
                                    barcode.put("format", "UNKNOWN");
                                }
                                if (!barcode.isEmpty()) {
                                    barcodes.add(barcode);
                                }
                            }
                        }
                    } else {
                        // Nếu không phải JSON, thử extract barcode từ text
                        // Tìm các chuỗi số hoặc alphanumeric có thể là barcode
                        String[] lines = content.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            // Tìm pattern giống barcode (ít nhất 4 ký tự)
                            if (line.matches(".*[A-Z0-9]{4,}.*")) {
                                String extracted = line.replaceAll(".*?([A-Z0-9]{4,}).*", "$1");
                                if (extracted.length() >= 4) {
                                    Map<String, String> barcode = new HashMap<>();
                                    barcode.put("text", extracted);
                                    barcode.put("format", "AI_DETECTED");
                                    barcodes.add(barcode);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing barcode response from AI", e);
        }
        return barcodes;
    }
}
 