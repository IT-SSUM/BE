package com.itssum.global.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * 인증 없이 호출하는 인증 API 에서는 Authorization 헤더를 읽지 않는다.
 * 앱이 만료된 액세스 토큰을 붙인 채 재발급·로그아웃을 불러도 401 로 막히지 않게 하기 위해서다.
 */
public class PublicPathBearerTokenResolver implements BearerTokenResolver {

    private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();
    private final Set<String> publicPaths;

    public PublicPathBearerTokenResolver(String... publicPaths) {
        this.publicPaths = Set.of(publicPaths);
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (publicPaths.contains(request.getRequestURI())) {
            return null;
        }
        return delegate.resolve(request);
    }
}
