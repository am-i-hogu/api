package com.hogu.am_i_hogu.domain.policy.controller;

import com.hogu.am_i_hogu.domain.policy.dto.response.PolicyResponse;
import com.hogu.am_i_hogu.domain.policy.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PolicyController implements PolicyApiDoc {

    private final PrivacyService privacyService;

    public PolicyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    /**
     * [ACCOUNT-001] 개인정보 처리 방침 조회
     *
     * @return 현재 적용 중인 개인정보 처리 방침 전문 및 메타데이터
     */
    @GetMapping("/api/policies/privacy")
    public ResponseEntity<PolicyResponse> getPrivacyPolicy() {
        return ResponseEntity.ok(privacyService.getPrivacyPolicy());
    }
}
