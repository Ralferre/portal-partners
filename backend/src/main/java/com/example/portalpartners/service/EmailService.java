package com.example.portalpartners.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendResetPasswordEmail(String to, String resetToken) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject("Recuperação de Senha - Portal Partners");
        String resetUrl = "http://localhost:8080/api/auth/reset-password?token=" + resetToken;  // Ajuste URL para frontend ou API
        helper.setText("""
                <html>
                <body>
                <p>Olá,</p>
                <p>Clique no link abaixo para resetar sua senha:</p>
                <a href="%s">Resetar Senha</a>
                <p>Se você não solicitou isso, ignore este email.</p>
                </body>
                </html>
                """.formatted(resetUrl), true);  // HTML para link clicável

        mailSender.send(message);
    }
}
