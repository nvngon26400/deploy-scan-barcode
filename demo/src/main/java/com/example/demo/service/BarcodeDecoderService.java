package com.example.demo.service;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.oned.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Service
public class BarcodeDecoderService {

    private final AdvancedImageProcessingService advancedImageProcessing;

    public BarcodeDecoderService(AdvancedImageProcessingService advancedImageProcessing) {
        this.advancedImageProcessing = advancedImageProcessing;
    }

    public List<Result> decode(byte[] imageBytes) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) {
            throw new IOException("Không thể đọc ảnh đầu vào.");
        }

        List<BufferedImage> candidates = generateVariants(original);
        
        // Thêm các ảnh đã xử lý bằng thuật toán nâng cao
        List<BufferedImage> advancedProcessed = advancedImageProcessing.processAdvanced(original);
        candidates.addAll(advancedProcessed);
        
        Map<DecodeHintType, Object> hints = buildHints();

        List<Result> results = new ArrayList<>();
        Set<String> seenTexts = new HashSet<>();

        for (BufferedImage img : candidates) {
            for (Result r : tryDecodeAll(img, hints)) {
                if (seenTexts.add(r.getText())) {
                    results.add(r);
                }
            }
        }

        return results;
    }

    private Map<DecodeHintType, Object> buildHints() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Arrays.asList(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.ITF,
                BarcodeFormat.PDF_417,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.AZTEC
        ));
        return hints;
    }

    private List<Result> tryDecodeAll(BufferedImage img, Map<DecodeHintType, Object> hints) {
        List<Result> out = new ArrayList<>();
        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(hints);

        // Single decode attempts
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmapHybrid = new BinaryBitmap(new HybridBinarizer(source));
            Result r = reader.decode(bitmapHybrid, hints);
            out.add(r);
        } catch (Exception ignored) {}

        try {
            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmapGlobal = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            Result r = reader.decode(bitmapGlobal, hints);
            out.add(r);
        } catch (Exception ignored) {}

        // Multiple decode (nhiều mã trong 1 ảnh)
        try {
            GenericMultipleBarcodeReader multi = new GenericMultipleBarcodeReader(reader);
            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmapHybrid = new BinaryBitmap(new HybridBinarizer(source));
            Result[] multiple = multi.decodeMultiple(bitmapHybrid, hints);
            if (multiple != null) {
                out.addAll(Arrays.asList(multiple));
            }
        } catch (Exception ignored) {}

        try {
            GenericMultipleBarcodeReader multi = new GenericMultipleBarcodeReader(reader);
            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmapGlobal = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            Result[] multiple = multi.decodeMultiple(bitmapGlobal, hints);
            if (multiple != null) {
                out.addAll(Arrays.asList(multiple));
            }
        } catch (Exception ignored) {}

        return out;
    }

    private List<BufferedImage> generateVariants(BufferedImage original) {
        List<BufferedImage> variants = new ArrayList<>();
        variants.add(original);

        // Thang xám
        BufferedImage gray = toGrayscale(original);
        variants.add(gray);

        // Xử lý ảnh lóa sáng - giảm highlight
        BufferedImage reducedGlare = reduceGlare(gray);
        variants.add(reducedGlare);

        // CLAHE (Contrast Limited Adaptive Histogram Equalization) - tốt cho ảnh lóa sáng
        BufferedImage clahe = applyCLAHE(gray);
        variants.add(clahe);
        variants.add(reduceGlare(clahe));

        // Gamma correction - điều chỉnh độ sáng
        BufferedImage gammaCorrected = applyGammaCorrection(gray, 0.7f);
        variants.add(gammaCorrected);
        BufferedImage gammaCorrected2 = applyGammaCorrection(gray, 1.5f);
        variants.add(gammaCorrected2);

        // Tăng tương phản nhẹ
        BufferedImage contrast = adjustContrast(gray, 1.25f, -10f);
        variants.add(contrast);

        // Tăng tương phản mạnh hơn cho ảnh lóa sáng
        BufferedImage highContrast = adjustContrast(reducedGlare, 1.5f, -20f);
        variants.add(highContrast);

        // Sharpen
        BufferedImage sharp = sharpen(contrast);
        variants.add(sharp);
        BufferedImage sharpGlare = sharpen(reducedGlare);
        variants.add(sharpGlare);

        // Adaptive thresholding - tốt cho ảnh có độ sáng không đều
        BufferedImage adaptive = applyAdaptiveThreshold(gray);
        variants.add(adaptive);
        BufferedImage adaptiveGlare = applyAdaptiveThreshold(reducedGlare);
        variants.add(adaptiveGlare);

        // Phóng to (upscale) nếu ảnh nhỏ
        if (Math.max(original.getWidth(), original.getHeight()) < 1000) {
            BufferedImage scaled2x = scale(sharp, 2.0);
            variants.add(scaled2x);
            BufferedImage scaled3x = scale(sharp, 3.0);
            variants.add(scaled3x);
            BufferedImage scaledGlare = scale(sharpGlare, 2.0);
            variants.add(scaledGlare);
        }

        // Thử các góc xoay (ảnh nghiêng/chói có thể cần)
        variants.add(rotate(original, 90));
        variants.add(rotate(original, 180));
        variants.add(rotate(original, 270));
        variants.add(rotate(reducedGlare, 90));
        variants.add(rotate(clahe, 90));

        // Thử invert (chói mạnh làm mã tối trên nền sáng hoặc ngược lại)
        variants.add(invert(gray));
        variants.add(invert(reducedGlare));
        variants.add(invert(clahe));

        return variants;
    }

    private BufferedImage toGrayscale(BufferedImage src) {
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        op.filter(src, dst);
        return dst;
    }

    private BufferedImage adjustContrast(BufferedImage src, float scale, float offset) {
        // scale > 1 tăng độ tương phản, offset dịch mức sáng
        RescaleOp op = new RescaleOp(scale, offset, null);
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        op.filter(src, dst);
        return dst;
    }

    private BufferedImage sharpen(BufferedImage src) {
        // Kernel sharpen đơn giản
        float[] kernel = {
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
        };
        Kernel k = new Kernel(3, 3, kernel);
        ConvolveOp op = new ConvolveOp(k, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        op.filter(src, dst);
        return dst;
    }

    private BufferedImage scale(BufferedImage src, double factor) {
        int w = (int) Math.round(src.getWidth() * factor);
        int h = (int) Math.round(src.getHeight() * factor);
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

    private BufferedImage rotate(BufferedImage src, double degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians)), cos = Math.abs(Math.cos(radians));
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        BufferedImage dst = new BufferedImage(newW, newH, src.getType());
        Graphics2D g2 = dst.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            AffineTransform at = new AffineTransform();
            at.translate(newW / 2.0, newH / 2.0);
            at.rotate(radians);
            at.translate(-w / 2.0, -h / 2.0);
            g2.drawRenderedImage(src, at);
        } finally {
            g2.dispose();
        }
        return dst;
    }

    private BufferedImage invert(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgba = src.getRGB(x, y);
                Color col = new Color(rgba, true);
                Color inv = new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue());
                dst.setRGB(x, y, inv.getRGB());
            }
        }
        return dst;
    }

    /**
     * Giảm lóa sáng bằng cách giảm các pixel quá sáng
     */
    private BufferedImage reduceGlare(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        int threshold = 200; // Ngưỡng pixel sáng
        float reductionFactor = 0.6f; // Giảm 40% độ sáng của pixel quá sáng

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                Color col = new Color(rgb, true);
                int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;

                if (gray > threshold) {
                    // Giảm độ sáng của pixel quá sáng
                    int newRed = Math.min(255, (int) (col.getRed() * reductionFactor));
                    int newGreen = Math.min(255, (int) (col.getGreen() * reductionFactor));
                    int newBlue = Math.min(255, (int) (col.getBlue() * reductionFactor));
                    Color newCol = new Color(newRed, newGreen, newBlue);
                    dst.setRGB(x, y, newCol.getRGB());
                } else {
                    dst.setRGB(x, y, rgb);
                }
            }
        }
        return dst;
    }

    /**
     * Áp dụng CLAHE (Contrast Limited Adaptive Histogram Equalization)
     * Tốt cho ảnh có độ sáng không đều hoặc lóa sáng
     */
    private BufferedImage applyCLAHE(BufferedImage src) {
        // Implement CLAHE đơn giản bằng cách chia ảnh thành các vùng nhỏ
        // và áp dụng histogram equalization cho từng vùng
        int tileSize = Math.min(64, Math.min(src.getWidth(), src.getHeight()) / 4);
        if (tileSize < 8) tileSize = 8;

        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        int tilesX = (src.getWidth() + tileSize - 1) / tileSize;
        int tilesY = (src.getHeight() + tileSize - 1) / tileSize;

        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                int x0 = tx * tileSize;
                int y0 = ty * tileSize;
                int x1 = Math.min(x0 + tileSize, src.getWidth());
                int y1 = Math.min(y0 + tileSize, src.getHeight());

                // Tính histogram cho tile này
                int[] histogram = new int[256];
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int rgb = src.getRGB(x, y);
                        Color col = new Color(rgb, true);
                        int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                        histogram[gray]++;
                    }
                }

                // Tính CDF (Cumulative Distribution Function)
                int[] cdf = new int[256];
                cdf[0] = histogram[0];
                for (int i = 1; i < 256; i++) {
                    cdf[i] = cdf[i - 1] + histogram[i];
                }

                // Áp dụng histogram equalization
                int pixelCount = (x1 - x0) * (y1 - y0);
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int rgb = src.getRGB(x, y);
                        Color col = new Color(rgb, true);
                        int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                        int newGray = (int) ((cdf[gray] * 255.0) / pixelCount);
                        newGray = Math.max(0, Math.min(255, newGray));
                        Color newCol = new Color(newGray, newGray, newGray);
                        dst.setRGB(x, y, newCol.getRGB());
                    }
                }
            }
        }
        return dst;
    }

    /**
     * Áp dụng gamma correction để điều chỉnh độ sáng
     */
    private BufferedImage applyGammaCorrection(BufferedImage src, float gamma) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        float invGamma = 1.0f / gamma;
        int[] lookupTable = new int[256];
        for (int i = 0; i < 256; i++) {
            lookupTable[i] = (int) (255.0 * Math.pow(i / 255.0, invGamma));
        }

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                Color col = new Color(rgb, true);
                int newRed = lookupTable[col.getRed()];
                int newGreen = lookupTable[col.getGreen()];
                int newBlue = lookupTable[col.getBlue()];
                Color newCol = new Color(newRed, newGreen, newBlue);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        return dst;
    }

    /**
     * Áp dụng adaptive thresholding - tốt cho ảnh có độ sáng không đều
     */
    private BufferedImage applyAdaptiveThreshold(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        int blockSize = 15; // Kích thước vùng để tính threshold
        int C = 5; // Hằng số trừ đi từ mean

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                // Tính mean của vùng xung quanh
                int sum = 0;
                int count = 0;
                int halfBlock = blockSize / 2;

                for (int dy = -halfBlock; dy <= halfBlock; dy++) {
                    for (int dx = -halfBlock; dx <= halfBlock; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && nx < src.getWidth() && ny >= 0 && ny < src.getHeight()) {
                            int rgb = src.getRGB(nx, ny);
                            Color col = new Color(rgb, true);
                            int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                            sum += gray;
                            count++;
                        }
                    }
                }

                int mean = count > 0 ? sum / count : 128;
                int threshold = mean - C;

                int rgb = src.getRGB(x, y);
                Color col = new Color(rgb, true);
                int gray = (col.getRed() + col.getGreen() + col.getBlue()) / 3;
                int newValue = (gray > threshold) ? 255 : 0;
                Color newCol = new Color(newValue, newValue, newValue);
                dst.setRGB(x, y, newCol.getRGB());
            }
        }
        return dst;
    }
}
