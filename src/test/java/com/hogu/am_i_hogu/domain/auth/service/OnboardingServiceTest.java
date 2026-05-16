package com.hogu.am_i_hogu.domain.auth.service;

import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.user.repository.UserHoguStatRepository;
import com.hogu.am_i_hogu.domain.auth.domain.RegisterSession;
import com.hogu.am_i_hogu.domain.auth.exception.AuthErrorCode;
import com.hogu.am_i_hogu.domain.auth.repository.RegisterSessionRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.SocialAccountRepository;
import com.hogu.am_i_hogu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class OnboardingServiceTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final RegisterSessionRepository registerSessionRepository = mock(RegisterSessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenHasher tokenHasher = mock(TokenHasher.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final TokenIssueService tokenIssueService = mock(TokenIssueService.class);
    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final UserHoguStatRepository userHoguStatRepository = mock(UserHoguStatRepository.class);
    private final OnboardingService onboardingService = new OnboardingService(
            jwtProvider,
            registerSessionRepository,
            userRepository,
            tokenHasher,
            tsidGenerator,
            tokenIssueService,
            socialAccountRepository,
            userHoguStatRepository
    );

    /**
     * register token이 비어있는 경우 테스트:
     * register token 쿠키 없이 온보딩 요청 시
     * EMPTY_REGISTER_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void createUserThrowsEmptyRegisterTokenWhenRegisterTokenIsMissing() {
        when(jwtProvider.validateRegisterToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        assertThatThrownBy(() -> onboardingService.createUser(null, "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMPTY_REGISTER_TOKEN));
    }

    /**
     * register token이 만료된 경우 테스트:
     * 만료된 register token으로 온보딩 요청 시
     * REGISTER_TOKEN_EXPIRED 예외가 발생하는지 확인
     */
    @Test
    void createUserThrowsRegisterTokenExpiredWhenRegisterTokenIsExpired() {
        when(jwtProvider.validateRegisterToken("expired-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.EXPIRED);

        assertThatThrownBy(() -> onboardingService.createUser("expired-register-token", "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REGISTER_TOKEN_EXPIRED));
    }

    /**
     * register token이 잘못된 경우 테스트:
     * 잘못된 register token으로 온보딩 요청 시
     * INVALID_REGISTER_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void createUserThrowsInvalidRegisterTokenWhenRegisterTokenIsInvalid() {
        when(jwtProvider.validateRegisterToken("invalid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.INVALID);

        assertThatThrownBy(() -> onboardingService.createUser("invalid-register-token", "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REGISTER_TOKEN));
    }

    /**
     * 닉네임이 비어있는 경우 테스트:
     * 비어있는 닉네임으로 온보딩 요청 시
     * INVALID_INPUT_VALUE 예외와 EMPTY_NICKNAME 상세 오류가 발생하는지 확인
     */
    @Test
    void createUserThrowsInvalidInputValueWhenNicknameIsEmpty() {
        RegisterSession registerSession = new RegisterSession(
                1L,
                100L,
                "saved-hash",
                LocalDateTime.now()
        );

        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(100L);
        when(registerSessionRepository.findFirstBySocialAccountIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(registerSession));
        when(tokenHasher.hash("valid-register-token"))
                .thenReturn("saved-hash");

        assertThatThrownBy(() -> onboardingService.createUser("valid-register-token", " "))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_INPUT_VALUE);
                    List<ErrorResponse.ErrorDetail> errors = exception.getErrors();
                    assertThat(errors.get(0).getField()).isEqualTo("nickname");
                    assertThat(errors.get(0).getCode()).isEqualTo("EMPTY_NICKNAME");
                });
    }

    /**
     * 닉네임에 특수문자가 포함된 경우 테스트:
     * 특수문자가 포함된 닉네임으로 온보딩 요청 시
     * INVALID_INPUT_VALUE 예외와 SPECIAL_CHAR_NICKNAME 상세 오류가 발생하는지 확인
     */
    @Test
    void createUserThrowsInvalidInputValueWhenNicknameContainsSpecialCharacter() {
        RegisterSession registerSession = new RegisterSession(
                1L,
                100L,
                "saved-hash",
                LocalDateTime.now()
        );

        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(100L);
        when(registerSessionRepository.findFirstBySocialAccountIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(registerSession));
        when(tokenHasher.hash("valid-register-token"))
                .thenReturn("saved-hash");

        assertThatThrownBy(() -> onboardingService.createUser("valid-register-token", "nickname!"))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_INPUT_VALUE);
                    List<ErrorResponse.ErrorDetail> errors = exception.getErrors();
                    assertThat(errors.get(0).getField()).isEqualTo("nickname");
                    assertThat(errors.get(0).getCode()).isEqualTo("SPECIAL_CHAR_NICKNAME");
                });
    }

    /**
     * 닉네임 길이가 잘못된 경우 테스트:
     * 길이 조건을 만족하지 않는 닉네임으로 온보딩 요청 시
     * INVALID_INPUT_VALUE 예외와 NICKNAME_LENGTH_EXCEEDED 상세 오류가 발생하는지 확인
     */
    @Test
    void createUserThrowsInvalidInputValueWhenNicknameLengthIsInvalid() {
        RegisterSession registerSession = new RegisterSession(
                1L,
                100L,
                "saved-hash",
                LocalDateTime.now()
        );

        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(100L);
        when(registerSessionRepository.findFirstBySocialAccountIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(registerSession));
        when(tokenHasher.hash("valid-register-token"))
                .thenReturn("saved-hash");

        assertThatThrownBy(() -> onboardingService.createUser("valid-register-token", "n"))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_INPUT_VALUE);
                    List<ErrorResponse.ErrorDetail> errors = exception.getErrors();
                    assertThat(errors.get(0).getField()).isEqualTo("nickname");
                    assertThat(errors.get(0).getCode()).isEqualTo("NICKNAME_LENGTH_EXCEEDED");
                });
    }
}
