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

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)          // CSRF 공격 방지 비활성화
                .formLogin(AbstractHttpConfigurer::disable)     // spring security의 기본 로그인 폼 사용 비활성화
                .sessionManagement(session ->       // spring security가 session 저장하지 않도록 함
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                        "/api/users/check-nickname",
                                        "/api/posts",
                                        "/api/posts/*",
                                        "/api/posts/*/comments",
                                        "/api/auth/login/*").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                        "/api/auth/logout",
                                        "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/**").anonymous()
                        .requestMatchers(HttpMethod.POST,"/api/users").anonymous()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex          // 인증 실패 시 jwtAuthenticationEntryPoint가 응답 처리
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)   // 필터 추가 위치 지정
                .build();
    }
}
