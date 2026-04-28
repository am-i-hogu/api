package com.hogu.am_i_hogu.domain.oauth.domain;

public enum OAuthProvider {
    GOOGLE;

    /**
     * 문자열 형태의 provider 이름을 Enum 객체로 변환
     * 지원하지 않는 provider일 경우 exception throw
     *
     * @param providerName 변환할 소셜 로그인 provider 이름
     * @return 매칭되는 OAuthProvider Enum 상수
     * @throws IllegalArgumentException 지원하지 않는 제공자 이름이 입력된 경우 발생
     */
    public static OAuthProvider from(String providerName) {
        return switch(providerName) {
            case "GOOGLE" -> GOOGLE;

            // HACK: custom exception 구현 전 임시로 IllegalArgumentException 사용
            default -> throw new IllegalArgumentException("Unsupported provider: " + providerName);
        };
    }
}
