package com.hogu.am_i_hogu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan													// config 클래스 탐색
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)		// JWT/OAuth 기반의 인증만 사용하므로 기본 사용자 자동 생성 비활성화
public class AmIHoguApplication {

	public static void main(String[] args) {
		SpringApplication.run(AmIHoguApplication.class, args);
	}

}
