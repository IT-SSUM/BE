package com.itssum.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.itssum.domain.chat.dto.response.ChatAppointmentResponse;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ChatAppointmentCodec {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public String encode(ChatAppointmentResponse appointment) {
        try {
            return objectMapper.writeValueAsString(appointment);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public ChatAppointmentResponse decode(String content) {
        try {
            return objectMapper.readValue(content, ChatAppointmentResponse.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
