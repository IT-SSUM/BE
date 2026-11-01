package com.itssum.domain.match.dto.request;

import jakarta.validation.constraints.Size;

public record MatchReasonRequest(@Size(max = 255) String reason) {
}
