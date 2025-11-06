package com.example.demo.service;

import com.example.demo.entity.BarcodeScan;
import com.example.demo.repository.BarcodeScanRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BarcodeScanService {

    private final BarcodeScanRepository barcodeScanRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public BarcodeScanService(BarcodeScanRepository barcodeScanRepository) {
        this.barcodeScanRepository = barcodeScanRepository;
    }

    public BarcodeScan saveBarcodeScan(MultipartFile imageFile, String analysisResult) throws IOException {
        // Save image file
        String imagePath = saveBarcodeImage(imageFile);

        // Create and save BarcodeScan record
        BarcodeScan barcodeScan = new BarcodeScan();
        barcodeScan.setBarcodeImg(imagePath);
        barcodeScan.setAnalysisResult(analysisResult);
        barcodeScan.setScannedAt(LocalDateTime.now());

        return barcodeScanRepository.save(barcodeScan);
    }

    private String saveBarcodeImage(MultipartFile imageFile) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir + "barcode-scans/");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFileName = imageFile.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String storedFileName = "BARCODE_" + UUID.randomUUID().toString() + fileExtension;

        // Save file to disk
        Path filePath = uploadPath.resolve(storedFileName);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    public byte[] exportToExcel() throws IOException {
        List<BarcodeScan> scans = barcodeScanRepository.findAllByOrderByScannedAtDesc();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Barcode Scans");

        // Create header row
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        String[] headers = {"STT", "Hình ảnh", "Đường dẫn ảnh", "Kết quả phân tích AI", "Thời gian scan"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Set column widths
        sheet.setColumnWidth(0, 2000);  // STT
        sheet.setColumnWidth(1, 15000);  // Hình ảnh - wider for images
        sheet.setColumnWidth(2, 8000);   // Đường dẫn ảnh
        sheet.setColumnWidth(3, 15000);  // Kết quả phân tích AI
        sheet.setColumnWidth(4, 6000);   // Thời gian scan

        // Create drawing patriarch for images
        XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();

        // Create data rows
        int rowNum = 1;
        for (BarcodeScan scan : scans) {
            Row row = sheet.createRow(rowNum);
            
            // Set row height for image display
            row.setHeightInPoints(60);
            
            // STT
            row.createCell(0).setCellValue(rowNum);
            
            // Hình ảnh (column 1) - Insert image if file exists
            Cell imageCell = row.createCell(1);
            String imagePath = scan.getBarcodeImg();
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    Path imgPath = Paths.get(imagePath);
                    if (Files.exists(imgPath)) {
                        // Read image bytes
                        byte[] imageBytes = Files.readAllBytes(imgPath);
                        
                        // Determine picture type based on file extension
                        // Apache POI supports: JPEG, PNG, EMF, WMF, DIB (GIF is not supported)
                        int pictureType = Workbook.PICTURE_TYPE_JPEG;
                        String lowerPath = imagePath.toLowerCase();
                        if (lowerPath.endsWith(".png")) {
                            pictureType = Workbook.PICTURE_TYPE_PNG;
                        }
                        // GIF files will be treated as JPEG since GIF is not supported
                        
                        // Add picture to workbook
                        int pictureIdx = workbook.addPicture(imageBytes, pictureType);
                        
                        // Create anchor to position image in cell
                        XSSFClientAnchor anchor = new XSSFClientAnchor();
                        anchor.setCol1(1); // Column index (0-based)
                        anchor.setCol2(2); // End column
                        anchor.setRow1(rowNum); // Row index (0-based)
                        anchor.setRow2(rowNum + 1); // End row
                        
                        // Create picture and resize it
                        org.apache.poi.xssf.usermodel.XSSFPicture picture = (org.apache.poi.xssf.usermodel.XSSFPicture) drawing.createPicture(anchor, pictureIdx);
                        
                        // Resize image to fit in cell (max width ~400 pixels, maintain aspect ratio)
                        picture.resize(1.0); // This will scale to fit anchor
                    } else {
                        imageCell.setCellValue("File not found");
                    }
                } catch (Exception e) {
                    log.warn("Failed to insert image for scan ID {}: {}", scan.getId(), e.getMessage());
                    imageCell.setCellValue("Error loading image");
                }
            } else {
                imageCell.setCellValue("No image path");
            }
            
            // Đường dẫn ảnh
            row.createCell(2).setCellValue(scan.getBarcodeImg() != null ? scan.getBarcodeImg() : "");
            
            // Kết quả phân tích AI
            Cell analysisCell = row.createCell(3);
            analysisCell.setCellValue(scan.getAnalysisResult() != null ? scan.getAnalysisResult() : "");
            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            analysisCell.setCellStyle(wrapStyle);
            
            // Thời gian scan
            row.createCell(4).setCellValue(scan.getScannedAt() != null ? 
                scan.getScannedAt().toString() : "");
            
            rowNum++;
        }

        // Convert workbook to byte array
        byte[] excelBytes;
        try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            workbook.write(outputStream);
            excelBytes = outputStream.toByteArray();
        } finally {
            workbook.close();
        }

        return excelBytes;
    }

    public List<BarcodeScan> getAllScans() {
        return barcodeScanRepository.findAllByOrderByScannedAtDesc();
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}

