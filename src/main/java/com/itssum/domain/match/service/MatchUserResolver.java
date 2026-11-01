package com.itssum.domain.match.service;

import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchUserResolver {

    private final UserRepository userRepository;

    public User requireUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId;
        try {
            userId = Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
