package br.ufpb.dsc.jobhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public PasswordResetMailService(JavaMailSender mailSender,
                                    @Value("${app.password-reset.email-enabled:false}") boolean enabled,
                                    @Value("${app.password-reset.from:no-reply@radartech.dev}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public void send(String recipient, String resetLink) {
        if (!enabled) {
            throw new IllegalStateException("O envio de recuperação de senha não está configurado.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Redefinição de senha — Radar Tech");
        message.setText("Recebemos uma solicitação para redefinir sua senha no Radar Tech.\n\n"
                + "Use o link abaixo nos próximos 30 minutos:\n" + resetLink + "\n\n"
                + "Se você não fez essa solicitação, ignore este e-mail. Sua senha continuará a mesma.");
        mailSender.send(message);
    }
}
