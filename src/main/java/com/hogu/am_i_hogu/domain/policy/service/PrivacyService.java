package com.hogu.am_i_hogu.domain.policy.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.policy.domain.PolicyRevisions;
import com.hogu.am_i_hogu.domain.policy.dto.response.PolicyResponse;
import com.hogu.am_i_hogu.domain.policy.repository.PolicyRevisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivacyService {

    private final PolicyRevisionRepository policyRevisionRepository;

    public PrivacyService(PolicyRevisionRepository policyRevisionRepository) {
        this.policyRevisionRepository = policyRevisionRepository;
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPrivacyPolicy() {
        PolicyRevisions privacyPolicy = policyRevisionRepository
                .findTopByPolicyTypeAndIsCurrentTrueOrderByUpdatedAtDesc("PRIVACY")
                .orElseThrow(() -> new CustomException(CommonErrorCode.SERVER_ERROR));

        return new PolicyResponse(
                privacyPolicy.getVersion(),
                privacyPolicy.getUpdatedAt(),
                privacyPolicy.getHtmlContent()
        );
    }
}
