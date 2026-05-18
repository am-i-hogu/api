package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.user.dto.HoguLevelShortInfo;
import com.hogu.am_i_hogu.domain.user.dto.UserInfoSummary;
import com.hogu.am_i_hogu.domain.user.dto.response.MyPageResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.HoguLevelRepository;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class MyPageService {

    private static final String NONE_HOGU_LEVEL = "NONE";
    private static final String NONE_DESCRIPTION = "레벨을 집계할 수 없습니다.";

    private final UserRepository userRepository;
    private final HoguLevelRepository hoguLevelRepository;

    public MyPageService(
            UserRepository userRepository,
            HoguLevelRepository hoguLevelRepository
    ) {
        this.userRepository = userRepository;
        this.hoguLevelRepository = hoguLevelRepository;
    }

    /**
     * 마이페이지 조회
     * 투표가 포함된 게시물 수가 5개 이상인 경우에만 호구 레벨 반환
     *
     * @param userId 조회 요청한 사용자 id
     * @return 사용자 프로필 정보 및 호구 레벨 정보
     */
    public MyPageResponse getMyPage(Long userId) {
        UserInfoSummary userInfoSummary = userRepository.findUserInfoSummaryByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (userInfoSummary.votedPostCount() < 5) {
            return createResponse(userInfoSummary);
        }

        HoguLevelShortInfo hoguLevelShortInfo = hoguLevelRepository.findShortHoguLevelByHoguIndex(userInfoSummary.hoguIndex())
                .orElseThrow(() -> new CustomException(CommonErrorCode.SERVER_ERROR));

        return createResponse(userInfoSummary, hoguLevelShortInfo);
    }

    // 투표 포함된 게시물이 5개 미만인 경우에 대한 응답 생성
    private MyPageResponse createResponse(UserInfoSummary userInfoSummary) {
        return new MyPageResponse(
                userInfoSummary.nickname(),
                userInfoSummary.profileImageUrl(),
                userInfoSummary.hoguIndex(),
                NONE_HOGU_LEVEL,
                NONE_DESCRIPTION
        );
    }

    // 투표 포함된 게시물이 5개 이상인 경우에 대한 응답 생성
    private MyPageResponse createResponse(
            UserInfoSummary userInfoSummary,
            HoguLevelShortInfo hoguLevelShortInfo
    ) {
        return new MyPageResponse(
                userInfoSummary.nickname(),
                userInfoSummary.profileImageUrl(),
                userInfoSummary.hoguIndex(),
                hoguLevelShortInfo.code(),
                hoguLevelShortInfo.shortDescription()
        );
    }
}
