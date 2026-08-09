package br.ufpb.dsc.jobhub.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PasswordResetMailServiceTest {

    @Test
    void sendsAPlainTextRecoveryMessageWithoutExposingSecrets() {
        JavaMailSender sender = mock(JavaMailSender.class);
        PasswordResetMailService service = new PasswordResetMailService(sender, true, "contato@radartech.dev");

        service.send("pessoa@example.com", "https://radartech.dev/redefinir-senha?token=seguro");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("contato@radartech.dev");
        assertThat(message.getValue().getTo()).containsExactly("pessoa@example.com");
        assertThat(message.getValue().getSubject()).contains("Redefinição de senha");
        assertThat(message.getValue().getText())
                .contains("30 minutos")
                .contains("token=seguro")
                .contains("ignore este e-mail");
    }

    @Test
    void refusesDeliveryWhenEmailIntegrationIsDisabled() {
        PasswordResetMailService service = new PasswordResetMailService(mock(JavaMailSender.class), false,
                "contato@radartech.dev");

        assertThatThrownBy(() -> service.send("pessoa@example.com", "https://example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não está configurado");
    }
}
