package com.ca06.api.event;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {

    private final ObjectMapper objectMapper;

    public QrCodeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public QrCodeResponse generate(Event event) {
        String payload = payload(event);
        return new QrCodeResponse(
                "EVENT_CHECKIN",
                event.getId(),
                event.getName(),
                event.getCheckInToken(),
                payload,
                imageData(payload));
    }

    private String payload(Event event) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "EVENT_CHECKIN",
                    "eventId", event.getId(),
                    "token", event.getCheckInToken()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível gerar o payload do QR Code.", exception);
        }
    }

    private String imageData(String payload) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    360,
                    360,
                    Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage image = new BufferedImage(360, 360, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar a imagem do QR Code.", exception);
        }
    }
}
