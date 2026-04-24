package com.hogu.am_i_hogu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// JWT/OAuth 기반의 인증만 사용하므로 기본 사용자 자동 생성 비활성화
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AmIHoguApplication {

	public static void main(String[] args) {
		SpringApplication.run(AmIHoguApplication.class, args);
	}

}
