package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * provider에 맞는 OAuth callback handler를 반환
 */
@Component
public class OAuthCallbackHandlerFactory {
    private final Map<OAuthProvider, OAuthCallbackHandler> handlerMap;

    public OAuthCallbackHandlerFactory(List<OAuthCallbackHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(OAuthCallbackHandler::supports, Function.identity()));
    }

    public OAuthCallbackHandler get(OAuthProvider provider) {
        OAuthCallbackHandler handler = handlerMap.get(provider);
        if (handler == null) {
            throw new CustomException(OAuthErrorCode.UNSUPPORTED_PROVIDER);
        }
        return handler;
    }
}
