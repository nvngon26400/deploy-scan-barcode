package com.example.demo.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý ảnh nâng cao với các thuật toán mạnh mẽ cho ảnh mờ/chói
 */
@Service
public class AdvancedImageProcessingService {

    /**
     * Xử lý ảnh với tất cả các kỹ thuật nâng cao
     */
    public List<BufferedImage> processAdvanced(BufferedImage original) {
        List<BufferedImage> processed = new ArrayList<>();
        
        BufferedImage gray = toGrayscale(original);
        
        // 1. Deblurring - làm rõ ảnh mờ
        processed.add(deblurWiener(gray));
        processed.add(deblurUnsharpMask(gray));
        
        // 2. Advanced denoising - giảm nhiễu
        processed.add(bilateralFilter(gray));
        processed.add(nonLocalMeansDenoise(gray));
        
        // 3. Morphological operations - tăng cường cấu trúc barcode
        BufferedImage denoised = bilateralFilter(gray);
        processed.add(morphologicalOpening(denoised));
        processed.add(morphologicalClosing(denoised));
        processed.add(morphologicalGradient(denoised));
        
        // 4. Multi-scale processing
        processed.addAll(multiScaleProcessing(gray));
        
        // 5. Edge enhancement nâng cao
        processed.add(edgeEnhancement(gray));
        processed.add(laplacianSharpening(gray));
        
        // 6. Kết hợp nhiều kỹ thuật
        BufferedImage combined = gray;
        combined = bilateralFilter(combined);
        combined = deblurUnsharpMask(combined);
        combined = edgeEnhancement(combined);
        processed.add(combined);
        
        return processed;
    }

    /**
     * Wiener filter deblurring - làm rõ ảnh mờ
     */
    public BufferedImage deblurWiener(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        // Simplified Wiener filter với motion blur kernel
        float[] kernel = {
            0.1f, 0.1f, 0.1f, 0.1f, 0.1f,
            0.1f, 0.1f, 0.1f, 0.1f, 0.1f
        };
        
        // Áp dụng inverse filter với regularization
        float noiseVar = 0.01f; // Noise variance
        float signalVar = 0.1f; // Signal variance
        
        for (int y = 2; y < height - 2; y++) {
            for (int x = 2; x < width - 2; x++) {
                float sum = 0;
                float kernelSum = 0;
                
                for (int ky = -2; ky <= 2; ky++) {
                    for (int kx = -2; kx <= 2; kx++) {
                        int px = x + kx;
                        int py = y + ky;
                        if (px >= 0 && px < width && py >= 0 && py < height) {
                            int rgb = src.getRGB(px, py);
                            Color col = new Color(rgb, true);
                            int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                            
                            float weight = 1.0f / (1.0f + noiseVar / signalVar);
                            sum += gray * weight;
                            kernelSum += weight;
                        }
                    }
                }
                
                int newGray = kernelSum > 0 ? (int) (sum / kernelSum) : 128;
                newGray = Math.max(0, Math.min(255, newGray));
                Color newCol = new Color(newGray, newGray, newGray);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        
        return dst;
    }

    /**
     * Unsharp masking - làm rõ ảnh mờ bằng cách tăng cường edge
     */
    public BufferedImage deblurUnsharpMask(BufferedImage src) {
        // Tạo blurred version
        BufferedImage blurred = gaussianBlur(src, 3);
        
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        float amount = 1.5f; // Độ mạnh của unsharp mask
        
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int originalRgb = src.getRGB(x, y);
                int blurredRgb = blurred.getRGB(x, y);
                
                Color origCol = new Color(originalRgb, true);
                Color blurCol = new Color(blurredRgb, true);
                
                int origGray = (origCol.getRed() + origCol.getGreen() + origCol.getBlue()) / 3;
                int blurGray = (blurCol.getRed() + blurCol.getGreen() + blurCol.getBlue()) / 3;
                
                // Unsharp mask: original + amount * (original - blurred)
                int newGray = (int) (origGray + amount * (origGray - blurGray));
                newGray = Math.max(0, Math.min(255, newGray));
                
                Color newCol = new Color(newGray, newGray, newGray);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        
        return dst;
    }

    /**
     * Bilateral filter - giảm nhiễu nhưng giữ edge
     */
    public BufferedImage bilateralFilter(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage dst = new BufferedImage(width, height, src.getType());
        
        int radius = 5;
        double sigmaColor = 50.0;
        double sigmaSpace = 50.0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sum = 0;
                double weightSum = 0;
                
                int centerRgb = src.getRGB(x, y);
                Color centerCol = new Color(centerRgb, true);
                int centerGray = (centerCol.getRed() + centerCol.getGreen() + centerCol.getBlue()) / 3;
                
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int rgb = src.getRGB(nx, ny);
                            Color col = new Color(rgb, true);
                            int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                            
                            // Tính weight dựa trên khoảng cách màu và không gian
                            double colorDist = Math.abs(gray - centerGray);
                            double spaceDist = Math.sqrt(dx * dx + dy * dy);
                            
                            double colorWeight = Math.exp(-(colorDist * colorDist) / (2 * sigmaColor * sigmaColor));
                            double spaceWeight = Math.exp(-(spaceDist * spaceDist) / (2 * sigmaSpace * sigmaSpace));
                            double weight = colorWeight * spaceWeight;
                            
                            sum += gray * weight;
                            weightSum += weight;
                        }
                    }
                }
                
                int newGray = weightSum > 0 ? (int) (sum / weightSum) : centerGray;
                newGray = Math.max(0, Math.min(255, newGray));
                Color newCol = new Color(newGray, newGray, newGray);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        
        return dst;
    }

    /**
     * Non-local means denoising - thuật toán mạnh cho giảm nhiễu
     */
    public BufferedImage nonLocalMeansDenoise(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage dst = new BufferedImage(width, height, src.getType());
        
        int patchSize = 3;
        int searchWindow = 7;
        double h = 10.0; // Filter parameter
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sum = 0;
                double weightSum = 0;
                
                // Lấy patch trung tâm
                int[] centerPatch = getPatch(src, x, y, patchSize);
                
                for (int sy = Math.max(0, y - searchWindow); sy < Math.min(height, y + searchWindow); sy++) {
                    for (int sx = Math.max(0, x - searchWindow); sx < Math.min(width, x + searchWindow); sx++) {
                        int[] patch = getPatch(src, sx, sy, patchSize);
                        
                        // Tính distance giữa 2 patches
                        double distance = patchDistance(centerPatch, patch);
                        double weight = Math.exp(-distance / (h * h));
                        
                        int rgb = src.getRGB(sx, sy);
                        Color col = new Color(rgb, true);
                        int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                        
                        sum += gray * weight;
                        weightSum += weight;
                    }
                }
                
                int newGray = weightSum > 0 ? (int) (sum / weightSum) : 128;
                newGray = Math.max(0, Math.min(255, newGray));
                Color newCol = new Color(newGray, newGray, newGray);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        
        return dst;
    }

    private int[] getPatch(BufferedImage img, int cx, int cy, int size) {
        int[] patch = new int[size * size];
        int idx = 0;
        int half = size / 2;
        
        for (int dy = -half; dy <= half; dy++) {
            for (int dx = -half; dx <= half; dx++) {
                int x = cx + dx;
                int y = cy + dy;
                
                if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) {
                    int rgb = img.getRGB(x, y);
                    Color col = new Color(rgb, true);
                    patch[idx] = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                } else {
                    patch[idx] = 128;
                }
                idx++;
            }
        }
        
        return patch;
    }

    private double patchDistance(int[] patch1, int[] patch2) {
        double sum = 0;
        for (int i = 0; i < patch1.length; i++) {
            double diff = patch1[i] - patch2[i];
            sum += diff * diff;
        }
        return sum / patch1.length;
    }

    /**
     * Morphological opening - loại bỏ nhiễu nhỏ
     */
    public BufferedImage morphologicalOpening(BufferedImage src) {
        return morphologicalDilation(morphologicalErosion(src));
    }

    /**
     * Morphological closing - lấp đầy lỗ hổng nhỏ
     */
    public BufferedImage morphologicalClosing(BufferedImage src) {
        return morphologicalErosion(morphologicalDilation(src));
    }

    /**
     * Morphological gradient - tăng cường edge
     */
    public BufferedImage morphologicalGradient(BufferedImage src) {
        BufferedImage dilated = morphologicalDilation(src);
        BufferedImage eroded = morphologicalErosion(src);
        
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int dilRgb = dilated.getRGB(x, y);
                int eroRgb = eroded.getRGB(x, y);
                Color dilCol = new Color(dilRgb, true);
                Color eroCol = new Color(eroRgb, true);
                
                int dilGray = (dilCol.getRed() + dilCol.getGreen() + dilCol.getBlue()) / 3;
                int eroGray = (eroCol.getRed() + eroCol.getGreen() + eroCol.getBlue()) / 3;
                
                int newGray = Math.abs(dilGray - eroGray);
                Color newCol = new Color(newGray, newGray, newGray);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        return dst;
    }

    private BufferedImage morphologicalErosion(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        int radius = 2;
        
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int min = 255;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && nx < src.getWidth() && ny >= 0 && ny < src.getHeight()) {
                            int rgb = src.getRGB(nx, ny);
                            Color col = new Color(rgb, true);
                            int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                            min = Math.min(min, gray);
                        }
                    }
                }
                Color newCol = new Color(min, min, min);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        return dst;
    }

    private BufferedImage morphologicalDilation(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        int radius = 2;
        
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int max = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && nx < src.getWidth() && ny >= 0 && ny < src.getHeight()) {
                            int rgb = src.getRGB(nx, ny);
                            Color col = new Color(rgb, true);
                            int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                            max = Math.max(max, gray);
                        }
                    }
                }
                Color newCol = new Color(max, max, max);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        return dst;
    }

    /**
     * Multi-scale processing - xử lý ở nhiều tỷ lệ khác nhau
     */
    public List<BufferedImage> multiScaleProcessing(BufferedImage src) {
        List<BufferedImage> results = new ArrayList<>();
        
        // Xử lý ở các scale khác nhau
        for (double scale : new double[]{0.5, 1.0, 1.5, 2.0}) {
            BufferedImage scaled = scaleImage(src, scale);
            BufferedImage processed = bilateralFilter(scaled);
            processed = edgeEnhancement(processed);
            results.add(processed);
        }
        
        return results;
    }

    /**
     * Edge enhancement nâng cao
     */
    public BufferedImage edgeEnhancement(BufferedImage src) {
        // Sobel edge detection
        float[] sobelX = {
            -1f, 0f, 1f,
            -2f, 0f, 2f,
            -1f, 0f, 1f
        };
        
        float[] sobelY = {
            -1f, -2f, -1f,
             0f,  0f,  0f,
             1f,  2f,  1f
        };
        
        BufferedImage edgesX = convolve(src, sobelX, 3);
        BufferedImage edgesY = convolve(src, sobelY, 3);
        
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgbX = edgesX.getRGB(x, y);
                int rgbY = edgesY.getRGB(x, y);
                Color colX = new Color(rgbX, true);
                Color colY = new Color(rgbY, true);
                
                int grayX = (colX.getRed() + colX.getGreen() + colX.getBlue()) / 3;
                int grayY = (colY.getRed() + colY.getGreen() + colY.getBlue()) / 3;
                
                int magnitude = (int) Math.sqrt(grayX * grayX + grayY * grayY);
                magnitude = Math.max(0, Math.min(255, magnitude));
                
                // Kết hợp với ảnh gốc
                int origRgb = src.getRGB(x, y);
                Color origCol = new Color(origRgb, true);
                int origGray = (origCol.getRed() + origCol.getGreen() + origCol.getBlue()) / 3;
                
                int newGray = Math.min(255, origGray + magnitude / 2);
                Color newCol = new Color(newGray, newGray, newGray);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        
        return dst;
    }

    /**
     * Laplacian sharpening - làm rõ edge
     */
    public BufferedImage laplacianSharpening(BufferedImage src) {
        float[] laplacian = {
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        };
        
        return convolve(src, laplacian, 3);
    }

    private BufferedImage convolve(BufferedImage src, float[] kernel, int kernelSize) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        int half = kernelSize / 2;
        
        for (int y = half; y < src.getHeight() - half; y++) {
            for (int x = half; x < src.getWidth() - half; x++) {
                float sum = 0;
                int idx = 0;
                
                for (int ky = -half; ky <= half; ky++) {
                    for (int kx = -half; kx <= half; kx++) {
                        int rgb = src.getRGB(x + kx, y + ky);
                        Color col = new Color(rgb, true);
                        int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                        sum += gray * kernel[idx];
                        idx++;
                    }
                }
                
                int newGray = (int) Math.max(0, Math.min(255, sum));
                Color newCol = new Color(newGray, newGray, newGray);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        
        return dst;
    }

    private BufferedImage gaussianBlur(BufferedImage src, int radius) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size * size];
        float sigma = radius / 3.0f;
        float sum = 0;
        
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float value = (float) Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                kernel[(y + radius) * size + (x + radius)] = value;
                sum += value;
            }
        }
        
        // Normalize
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }
        
        return convolve(src, kernel, size);
    }

    private BufferedImage scaleImage(BufferedImage src, double factor) {
        int w = (int) (src.getWidth() * factor);
        int h = (int) (src.getHeight() * factor);
        BufferedImage dst = new BufferedImage(w, h, src.getType());
        Graphics2D g2 = dst.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(src, 0, 0, w, h, null);
        } finally {
            g2.dispose();
        }
        return dst;
    }

    private BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = dst.createGraphics();
        try {
            g2.drawImage(src, 0, 0, null);
        } finally {
            g2.dispose();
        }
        return dst;
    }
}

