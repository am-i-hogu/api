package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.user.dto.HoguLevelInfo;
import com.hogu.am_i_hogu.domain.user.dto.UserInfoSummary;
import com.hogu.am_i_hogu.domain.user.dto.response.MyPageResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.HoguLevelRepository;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class MyPageService {

    private final UserRepository userRepository;
    private final HoguLevelRepository hoguLevelRepository;

    public MyPageService(
            UserRepository userRepository,
            HoguLevelRepository hoguLevelRepository
    ) {
        this.userRepository = userRepository;
        this.hoguLevelRepository = hoguLevelRepository;
    }

    public MyPageResponse getMyPage(Long userId) {
        UserInfoSummary userInfoSummary = userRepository.findMyPageSummaryByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (userInfoSummary.votedPostCount() <= 10) {
            return createResponse(userInfoSummary);
        }

        HoguLevelInfo hoguLevelInfo = hoguLevelRepository.findHoguLevelByHoguIndex(userInfoSummary.hoguIndex())
                .orElseThrow(() -> new CustomException(CommonErrorCode.SERVER_ERROR));

        return createResponse(userInfoSummary, hoguLevelInfo);
    }

    private MyPageResponse createResponse(UserInfoSummary userInfoSummary) {
        return new MyPageResponse(
                userInfoSummary.nickname(),
                userInfoSummary.profileImageUrl(),
                userInfoSummary.hoguIndex(),
                "NONE",
                "레벨을 집계할 수 없습니다."
        );
    }

    private MyPageResponse createResponse(
            UserInfoSummary userInfoSummary,
            HoguLevelInfo hoguLevelInfo
    ) {
        return new MyPageResponse(
                userInfoSummary.nickname(),
                userInfoSummary.profileImageUrl(),
                userInfoSummary.hoguIndex(),
                hoguLevelInfo.displayName(),
                hoguLevelInfo.description()
        );
    }
}
