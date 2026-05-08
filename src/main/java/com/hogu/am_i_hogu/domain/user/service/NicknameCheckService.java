package com.hogu.am_i_hogu.domain.user.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.domain.user.dto.response.CheckNicknameResponse;
import com.hogu.am_i_hogu.domain.user.exception.UserErrorCode;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NicknameCheckService {
    private final UserRepository userRepository;

    public NicknameCheckService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CheckNicknameResponse checkNickname(String nickname) {
        validateNickname(nickname);
        boolean isAvailable = isNicknameAvailable(nickname);

        return new CheckNicknameResponse(isAvailable);
    }

    /**
     * 닉네임 검증
     * - 비어있는 경우
     * - 특수문자가 포함된 경우
     * - 길이 초과/미달인 경우
     *
     * @param nickname 요청으로 들어온 닉네임
     */
    private void validateNickname(String nickname) {
        List<ErrorResponse.ErrorDetail> errors = new ArrayList<>();

        if (nickname == null || nickname.isBlank()) {               // 닉네임이 비어있는 경우
            errors.add(new ErrorResponse.ErrorDetail(
                    "nickname",
                    "EMPTY_NICKNAME"
            ));
            throw new CustomException(UserErrorCode.INVALID_INPUT_VALUE, errors);
        }

        if (!nickname.matches("^[가-힣a-zA-Z0-9]+$")) {        // 닉네임에 특수문자가 포함된 경우
            errors.add(new ErrorResponse.ErrorDetail(
                    "nickname",
                    "SPECIAL_CHAR_NICKNAME"
            ));
        }
        if (nickname.length() < 2 || nickname.length() > 20) {      // 닉네임 길이 위반한 경우
            errors.add(new ErrorResponse.ErrorDetail(
                    "nickname",
                    "NICKNAME_LENGTH_EXCEEDED"
            ));
        }

        if (!errors.isEmpty()) {
            throw new CustomException(UserErrorCode.INVALID_INPUT_VALUE, errors);
        }
    }

    // 닉네임 중복 여부 검증
    private boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }
}
