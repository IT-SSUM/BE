package com.itssum.global.infra.mail;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class EmailSenderTest {

    @Test
    void sendsPlainTextMailFromConfiguredAccount() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailSender sender = new EmailSender(mailSender);
        ReflectionTestUtils.setField(sender, "from", "itssum@gmail.com");

        sender.send("donor@example.com", "제목", "본문");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertEquals("itssum@gmail.com", message.getValue().getFrom());
        assertArrayEquals(new String[] {"donor@example.com"}, message.getValue().getTo());
        assertEquals("제목", message.getValue().getSubject());
        assertEquals("본문", message.getValue().getText());
    }
}
