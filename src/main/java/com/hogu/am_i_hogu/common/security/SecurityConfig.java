package com.hogu.am_i_hogu.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)          // CSRF 공격 방지 비활성화
                .formLogin(AbstractHttpConfigurer::disable)     // spring security의 기본 로그인 폼 사용 비활성화
                .logout(AbstractHttpConfigurer::disable)        // spring security의 기본 로그아웃 기능 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)     // httpBasic 비활성
                .sessionManagement(session ->       // spring security가 session 저장하지 않도록 함
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Authentication 유무 상관 없는 경우
                        .requestMatchers(HttpMethod.GET,
                                        "/api/users/check-nickname",                        // USER-002: 닉네임 중복 체크
                                        "/api/posts",                                       // HOME-001: 홈 화면 조회
                                        "/api/posts/*",                                     // POST-001: 게시물 상세 조회
                                        "/api/posts/*/comments").permitAll()                // CI-001: 집단지성 조회
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()   // ACCOUNT-002: 로그아웃
                        // Authentication 없어야 하는 경우
                        .requestMatchers("/api/auth/**",                                    // AUTH-001~AUTH-003: 소셜로그인 관련
                                         "/api/users").anonymous()                          // ONBOARDING-001: 온보딩
                        // Authentication 있어야 하는 경우
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex              // 401, 403 공통 예외 처리 등록
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)   // 필터 추가 위치 지정
                .build();
    }
}
