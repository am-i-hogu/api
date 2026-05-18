package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.user.dto.HoguLevelInfo;
import com.hogu.am_i_hogu.domain.user.dto.MyPageSummary;
import com.hogu.am_i_hogu.domain.user.dto.response.MyPageResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.HoguLevelRepository;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class MyPageQueryService {

    private final UserRepository userRepository;
    private final HoguLevelRepository hoguLevelRepository;

    public MyPageQueryService(UserRepository userRepository, HoguLevelRepository hoguLevelRepository) {
        this.userRepository = userRepository;
        this.hoguLevelRepository = hoguLevelRepository;
    }

    public MyPageResponse getMyPage(Long userId) {
        MyPageSummary myPageSummary = userRepository.findMyPageSummaryByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (myPageSummary.votedPostCount() <= 10) {
            return createResponse(myPageSummary);
        }

        HoguLevelInfo hoguLevelInfo = hoguLevelRepository.findHoguLevelByHoguIndex(myPageSummary.hoguIndex())
                .orElseThrow(() -> new CustomException(CommonErrorCode.SERVER_ERROR));

        return createResponse(myPageSummary, hoguLevelInfo);
    }

    private MyPageResponse createResponse(MyPageSummary myPageSummary) {
        return new MyPageResponse(
                myPageSummary.nickname(),
                myPageSummary.profileImageUrl(),
                myPageSummary.hoguIndex(),
                "NONE",
                "레벨을 집계할 수 없습니다."
        );
    }

    private MyPageResponse createResponse(
            MyPageSummary myPageSummary,
            HoguLevelInfo hoguLevelInfo
    ) {
        return new MyPageResponse(
                myPageSummary.nickname(),
                myPageSummary.profileImageUrl(),
                myPageSummary.hoguIndex(),
                hoguLevelInfo.displayName(),
                hoguLevelInfo.description()
        );
    }
}
