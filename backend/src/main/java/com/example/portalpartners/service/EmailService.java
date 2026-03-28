package com.example.portalpartners.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${mail.from:no-reply@portal-partners.local}")
    private String mailFrom;

    public void sendResetPasswordEmail(String to, String resetToken) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject("Recuperação de Senha - Portal Partners");
        String resetUrl = frontendUrl
                + "/reset-password?token=" + URLEncoder.encode(resetToken, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(to, StandardCharsets.UTF_8);

        String resetUrlHtml = resetUrl.replace("&", "&amp;");

        String text = """
                Olá,

                Use o link abaixo para redefinir sua senha:
                %s

                Se você não solicitou isso, ignore este email.
                """.formatted(resetUrl);

        String html = """
                <html>
                <body>
                <p>Olá,</p>
                <p>Clique no link abaixo para resetar sua senha:</p>
                <a href="%s">Resetar Senha</a>
                <p>Se o link não abrir, copie e cole no navegador:</p>
                <p>%s</p>
                <p>Se você não solicitou isso, ignore este email.</p>
                </body>
                </html>
                """.formatted(resetUrlHtml, resetUrlHtml);

        helper.setText(text, html);

        mailSender.send(message);
    }
}
