package qrcodeapi;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @RestController
    @RequestMapping("/api")
    public class HealthQR {

        private static final Set<String> SUPPORTED_TYPES = Set.of("png", "jpeg", "gif");
        private static final Set<String> SUPPORTED_CORRECTIONS = Set.of("L", "M", "Q", "H");

        @GetMapping("/health")
        @ResponseStatus(HttpStatus.OK)
        public void healthCheck() {
        }

        @GetMapping(path = "/qrcode")
        public ResponseEntity<?> getQRCode(
                @RequestParam String contents,
                @RequestParam(defaultValue = "250") int size,
                @RequestParam(defaultValue = "png") String type,
                @RequestParam(defaultValue = "L") String correction) {

            // Validate contents parameter
            if (contents == null || contents.trim().isEmpty()) {
                return badRequest("Contents cannot be null or blank");
            }

            // Validate size parameter
            if (size < 150 || size > 350) {
                return badRequest("Image size must be between 150 and 350 pixels");
            }

            // Validate correction parameter
            if (!SUPPORTED_CORRECTIONS.contains(correction.toUpperCase())) {
                return badRequest("Permitted error correction levels are L, M, Q, H");
            }

            // Validate type parameter
            if (!SUPPORTED_TYPES.contains(type.toLowerCase())) {
                return badRequest("Only png, jpeg and gif image types are supported");
            }

            try {
                // Generate QR code image
                BufferedImage bufferedImage = generateQRCode(contents, size, correction);
                // Convert image to byte array
                byte[] imageBytes = convertToBytes(bufferedImage, type);
                // Determine media type
                MediaType mediaType = getMediaType(type);
                // Return response with image
                return ResponseEntity.ok().contentType(mediaType).body(imageBytes);
            } catch (WriterException | IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        // Helper method to return a bad request response
        private ResponseEntity<String> badRequest(String message) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + message + "\"}");
        }

        // Helper method to generate QR code image
        private BufferedImage generateQRCode(String contents, int size, String correction) throws WriterException {
            QRCodeWriter writer = new QRCodeWriter();
            ErrorCorrectionLevel errorCorrectionLevel = switch (correction.toUpperCase()) {
                case "M" -> ErrorCorrectionLevel.M;
                case "Q" -> ErrorCorrectionLevel.Q;
                case "H" -> ErrorCorrectionLevel.H;
                default -> ErrorCorrectionLevel.L;
            };
            Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            BitMatrix bitMatrix = writer.encode(contents, BarcodeFormat.QR_CODE, size, size, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        }

        // Helper method to convert BufferedImage to byte array
        private byte[] convertToBytes(BufferedImage bufferedImage, String type) throws IOException {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(bufferedImage, type, baos);
                return baos.toByteArray();
            }
        }

        // Helper method to determine media type based on file extension
        private MediaType getMediaType(String type) {
            return switch (type.toLowerCase()) {
                case "jpeg" -> MediaType.IMAGE_JPEG;
                case "gif" -> MediaType.IMAGE_GIF;
                default -> MediaType.IMAGE_PNG;
            };
        }
    }
}