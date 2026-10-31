package com.itssum.domain.auth.service;

import java.util.Locale;

/** 이메일 인증·가입·로그인에서 같은 기준으로 비교하도록 앞뒤 공백을 지우고 소문자로 맞춘다. */
final class EmailNormalizer {

    private EmailNormalizer() {
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
