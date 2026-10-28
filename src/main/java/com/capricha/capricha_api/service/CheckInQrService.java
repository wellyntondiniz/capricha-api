package com.capricha.capricha_api.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.rest.CheckInException;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;

/** Infraestrutura única de QR/autorização para eventos e palestras. */
@Service
public class CheckInQrService {
    public void exigirAdministrador(Usuario usuario) {
        if (!"ADMIN".equalsIgnoreCase(usuario.getPerfil()) && !"ADMINISTRADOR".equalsIgnoreCase(usuario.getPerfil())) {
            throw new CheckInException(403, "FORBIDDEN", "Somente administradores podem gerar o QR Code.");
        }
    }

    public void validarToken(String esperado, String recebido, String codigo, String mensagem) {
        if (esperado == null || recebido == null || !MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8))) {
            throw new CheckInException(400, codigo, mensagem);
        }
    }

    public String imagem(String payload) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 360, 360,
                    Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage imagem = new BufferedImage(360, 360, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 360; x++) {
                for (int y = 0; y < 360; y++) imagem.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(imagem, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new CheckInException(500, "QR_GENERATION_ERROR", "Não foi possível gerar o QR Code.");
        }
    }
}
