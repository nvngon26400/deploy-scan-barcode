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

    public List<Result> decode(byte[] imageBytes) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) {
            throw new IOException("Không thể đọc ảnh đầu vào.");
        }

        List<BufferedImage> candidates = generateVariants(original);
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

        // Tăng tương phản nhẹ
        BufferedImage contrast = adjustContrast(gray, 1.25f, -10f);
        variants.add(contrast);

        // Sharpen
        BufferedImage sharp = sharpen(contrast);
        variants.add(sharp);

        // Phóng to (upscale) nếu ảnh nhỏ
        if (Math.max(original.getWidth(), original.getHeight()) < 1000) {
            BufferedImage scaled2x = scale(sharp, 2.0);
            variants.add(scaled2x);
            BufferedImage scaled3x = scale(sharp, 3.0);
            variants.add(scaled3x);
        }

        // Thử các góc xoay (ảnh nghiêng/chói có thể cần)
        variants.add(rotate(original, 90));
        variants.add(rotate(original, 180));
        variants.add(rotate(original, 270));

        // Thử invert (chói mạnh làm mã tối trên nền sáng hoặc ngược lại)
        variants.add(invert(gray));

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
}
