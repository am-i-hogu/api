package com.hogu.am_i_hogu.domain.policy.controller;

import com.hogu.am_i_hogu.domain.policy.dto.response.PolicyResponse;
import com.hogu.am_i_hogu.domain.policy.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PolicyController {

    private final PrivacyService privacyService;

    public PolicyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @GetMapping("/api/policies/privacy")
    public ResponseEntity<PolicyResponse> getPrivacyPolicy() {
        return ResponseEntity.ok(privacyService.getPrivacyPolicy());
    }
}
