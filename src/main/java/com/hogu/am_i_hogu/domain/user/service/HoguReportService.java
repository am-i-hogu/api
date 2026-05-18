package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.domain.post.repository.PostRepository;
import com.hogu.am_i_hogu.domain.user.dto.*;
import com.hogu.am_i_hogu.domain.user.dto.response.CategoryAnalysisResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.HoguReportResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.HoguLevelRepository;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HoguReportService {

    private static final String NONE_HOGU_LEVEL = "NONE";
    private static final String NONE_DESCRIPTION = "레벨을 집계할 수 없습니다.";

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final HoguLevelRepository hoguLevelRepository;

    public HoguReportService(
            UserRepository userRepository,
            PostRepository postRepository,
            HoguLevelRepository hoguLevelRepository
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.hoguLevelRepository = hoguLevelRepository;
    }

    public HoguReportResponse getHoguReport(Long userId) {
        UserInfoSummary userInfoSummary = userRepository.findMyPageSummaryByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        List<PostVoteSummary> postVoteSummary = postRepository.findPostAnalysisByUserId(userId);
        int totalPostCount = postVoteSummary.size();
        int hoguPostCount = countHoguPosts(postVoteSummary);
        int notHoguPostCount = countNotHoguPosts(postVoteSummary);

        HoguLevelInfo hoguLevelInfo = createHoguLevelResponse(
                userInfoSummary.votedPostCount(),
                userInfoSummary.hoguIndex()
        );

        List<CategoryAnalysisResponse> categoryAnalysis =
                createCategoryAnalysis(hoguLevelInfo.code(), userId);

        return new HoguReportResponse(
                userInfoSummary.nickname(),
                userInfoSummary.profileImageUrl(),
                userInfoSummary.hoguIndex(),
                hoguLevelInfo.code(),
                hoguLevelInfo.shortDescription(),
                hoguLevelInfo.description(),
                categoryAnalysis,
                totalPostCount,
                hoguPostCount,
                notHoguPostCount
        );
    }

    private CategoryAnalysisResponse toCategoryAnalysisResponse(CategoryAnalysisSummary summary) {
        int hoguIndex = calculateHoguIndex(summary.hoguVoteCount(), summary.totalVoteCount());
        HoguLevelInfo simpleHoguLevelInfo = hoguLevelRepository.findHoguLevelByHoguIndex(hoguIndex)
                .orElseThrow(() -> new CustomException(CommonErrorCode.SERVER_ERROR));

        return new CategoryAnalysisResponse(
                summary.category(),
                hoguIndex,
                simpleHoguLevelInfo.code()
        );
    }

    private int calculateHoguIndex(long hoguVoteCount, long totalVoteCount) {
        if (totalVoteCount == 0) {
            return 0;
        }
        return Math.toIntExact(Math.round(hoguVoteCount * 100.0 / totalVoteCount));
    }

    private int countHoguPosts(List<PostVoteSummary> postVoteSummary) {
        return Math.toIntExact(postVoteSummary.stream()
                .filter(summary -> summary.hoguVoteCount() > summary.notHoguVoteCount())
                .count());
    }

    private int countNotHoguPosts(List<PostVoteSummary> postVoteSummary) {
        return Math.toIntExact(postVoteSummary.stream()
                .filter(summary -> summary.hoguVoteCount() < summary.notHoguVoteCount())
                .count());
    }

    private HoguLevelInfo createHoguLevelResponse(int votedPostCount, int hoguIndex) {
        if (votedPostCount < 5) {
            return new HoguLevelInfo(
                    NONE_HOGU_LEVEL,
                    NONE_DESCRIPTION,
                    NONE_DESCRIPTION
            );
        }
        HoguLevelInfo hoguLevelInfo = hoguLevelRepository.findHoguLevelByHoguIndex(hoguIndex)
                .orElseThrow(() -> new CustomException(CommonErrorCode.SERVER_ERROR));

        return hoguLevelInfo;
    }

    private List<CategoryAnalysisResponse> createCategoryAnalysis(String code, Long userId) {
        if (code.equals(NONE_HOGU_LEVEL)) {
            return List.of();
        }

        List<CategoryAnalysisSummary> categoryAnalysisSummary =
                        postRepository.findCategoryAnalysisByUserId(userId);
        return categoryAnalysisSummary.stream()
                .map(this::toCategoryAnalysisResponse)
                .toList();
    }
}
