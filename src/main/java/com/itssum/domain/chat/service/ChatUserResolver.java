package com.itssum.domain.chat.service;

import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserStatus;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatUserResolver {

    private final UserRepository userRepository;

    /** 인증 계층이 principal 이름으로 회원 ID 또는 이메일을 제공하면 그대로 연동된다. */
    public User requireUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String name = principal.getName();
        User user;
        try {
            user = userRepository.findById(Long.parseLong(name))
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        } catch (NumberFormatException ignored) {
            user = userRepository.findByEmail(name)
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
