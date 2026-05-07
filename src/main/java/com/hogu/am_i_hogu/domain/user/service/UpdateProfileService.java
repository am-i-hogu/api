package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.domain.user.domain.User;
import com.hogu.am_i_hogu.domain.user.dto.request.UpdateProfileRequest;
import com.hogu.am_i_hogu.domain.user.dto.response.UpdateProfileResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.hogu.am_i_hogu.domain.user.service.UpdateProfileService.ProfileImageRequestType.*;

@Service
public class UpdateProfileService {

    private final UserRepository userRepository;

    public UpdateProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public enum ProfileImageRequestType {
        KEEP,
        UPDATE,
        DELETE;
    }

    @Transactional
    public UpdateProfileResponse updateProfile(
            Long userId,
            UpdateProfileRequest request
    ) {
        if (request == null) {
            throw new CustomException(UserErrorCode.EMPTY_REQUEST_BODY);
        }

        ProfileImageRequestType imageRequestType = resolveProfileImageRequest(request);
        validate(request, imageRequestType);

        String nickname = request.nickname();
        String profileImageUrl = extractProfileImageUrl(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        updateProfile(user, nickname, profileImageUrl, imageRequestType);

        return new UpdateProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }

    private void validate(UpdateProfileRequest request, ProfileImageRequestType imageRequestType) {

        if (request.nickname() == null
                && imageRequestType == KEEP) {
            throw new CustomException(UserErrorCode.EMPTY_REQUEST_BODY);
        }

        List<ErrorResponse.ErrorDetail> errors = new ArrayList<>();
        validateNickname(request, errors);
        validateProfileImageUrl(request, imageRequestType, errors);

        if (!errors.isEmpty()) {
            throw new CustomException(UserErrorCode.INVALID_INPUT_VALUE, errors);
        }
    }

    // 닉네임 검증
    private void validateNickname(
            UpdateProfileRequest request,
            List<ErrorResponse.ErrorDetail> errors
    ) {
        String nickname = request.nickname();

        // 닉네임 필드가 들어오지 않은 경우
        if (nickname == null) {
            return;
        }

        // 닉네임이 비어있는 경우
        if (nickname.isBlank()) {
            errors.add(new ErrorResponse.ErrorDetail(
                    "nickname",
                    "EMPTY_NICKNAME"
            ));
            return;
        }

        // 닉네임에 특수문자가 포함된 경우
        if (!nickname.matches("^[가-힣a-zA-Z0-9]+$")) {
            errors.add(new ErrorResponse.ErrorDetail(
                    "nickname",
                    "SPECIAL_CHAR_NICKNAME"
            ));
        }

        // 닉네임 길이 위반한 경우
        if (nickname.length() < 2 || nickname.length() > 20) {
            errors.add(new ErrorResponse.ErrorDetail(
                    "nickname",
                    "NICKNAME_LENGTH_EXCEEDED"
            ));
        }
    }

    private String extractProfileImageUrl(UpdateProfileRequest request) {
        return request.profileImageUrl() == null ? null : request.profileImageUrl().orElse(null);
    }

    // 프로필 사진 url 검증
    private void validateProfileImageUrl(
            UpdateProfileRequest request,
            ProfileImageRequestType imageRequestType,
            List<ErrorResponse.ErrorDetail> errors
    ) {
        // 유지/삭제 요청은 url이 들어오지 않거나 null로 들어오므로 검증 스킵
        if (imageRequestType == KEEP
                || imageRequestType == DELETE) {
            return;
        }

        String profileImageUrl = extractProfileImageUrl(request);
        // 이미지 필드가 비어있는 경우
        if (profileImageUrl.isBlank()) {
            errors.add(new ErrorResponse.ErrorDetail(
                    "images",
                    "EMPTY_IMAGE_URL"
            ));
            return;
        }

        // TODO: S3 연결 후 이미지 url 유효성 검증 추가
        // url 형식이 올바르지 않은 경우
        if (!isValidImageUrl(profileImageUrl)) {
            errors.add(new ErrorResponse.ErrorDetail(
                    "images",
                    "INVALID_IMAGE_URL"
            ));
        }
    }

    private boolean isValidImageUrl(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    // 프로필 사진 삭제/업데이트/유지 요청 구분
    private ProfileImageRequestType resolveProfileImageRequest(UpdateProfileRequest request) {

        if (request.profileImageUrl() == null || !request.profileImageUrl().isPresent()) {
            return KEEP;
        }

        String profileImageUrl = request.profileImageUrl().orElse(null);
        if (profileImageUrl == null) {
            return DELETE;
        }

        return UPDATE;
    }

    private void updateProfile(
            User user,
            String nickname,
            String profileImageUrl,
            ProfileImageRequestType imageRequestType
    ) {
        LocalDateTime now = LocalDateTime.now();

        user.updateNickname(nickname, now);
        switch (imageRequestType) {
            case KEEP -> {}
            case UPDATE -> user.updateProfileImage(profileImageUrl, now);
            case DELETE -> user.updateProfileImage(null, now);
        }
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(UserErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
