package com.hogu.am_i_hogu.domain.auth;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.User.domain.UserHoguStat;
import com.hogu.am_i_hogu.domain.User.repository.UserHoguStatRepository;
import com.hogu.am_i_hogu.domain.auth.domain.RefreshToken;
import com.hogu.am_i_hogu.domain.auth.domain.RegisterSession;
import com.hogu.am_i_hogu.domain.oauth.domain.SocialAccount;
import com.hogu.am_i_hogu.domain.User.domain.User;
import com.hogu.am_i_hogu.domain.oauth.domain.OAuthProvider;
import com.hogu.am_i_hogu.domain.auth.dto.response.OnboardingResult;
import com.hogu.am_i_hogu.domain.auth.exception.AuthErrorCode;
import com.hogu.am_i_hogu.domain.auth.repository.RefreshTokenRepository;
import com.hogu.am_i_hogu.domain.auth.repository.RegisterSessionRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.SocialAccountRepository;
import com.hogu.am_i_hogu.domain.User.repository.UserRepository;
import com.hogu.am_i_hogu.domain.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final RegisterSessionRepository registerSessionRepository = mock(RegisterSessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenHasher tokenHasher = mock(TokenHasher.class);
    private final TsidGenerator tsidGenerator = mock(TsidGenerator.class);
    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final UserHoguStatRepository userHoguStatRepository = mock(UserHoguStatRepository.class);
    private final AuthService authService = new AuthService(
            jwtProvider,
            registerSessionRepository,
            userRepository,
            tokenHasher,
            tsidGenerator,
            socialAccountRepository,
            refreshTokenRepository,
            userHoguStatRepository
    );

    /**
     * register token이 비어있는 경우 테스트:
     * register token 쿠키 없이 온보딩 요청 시
     * EMPTY_REGISTER_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void emptyRegisterTokenTest() {
        when(jwtProvider.validateRegisterToken(null))
                .thenReturn(JwtProvider.TokenValidationResult.EMPTY);

        assertThatThrownBy(() -> authService.createUser(null, "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMPTY_REGISTER_TOKEN));
    }

    /**
     * register token이 만료된 경우 테스트:
     * 만료된 register token으로 온보딩 요청 시
     * REGISTER_TOKEN_EXPIRED 예외가 발생하는지 확인
     */
    @Test
    void expiredRegisterTokenTest() {
        when(jwtProvider.validateRegisterToken("expired-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.EXPIRED);

        assertThatThrownBy(() -> authService.createUser("expired-register-token", "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REGISTER_TOKEN_EXPIRED));
    }

    /**
     * register token이 잘못된 경우 테스트:
     * 잘못된 register token으로 온보딩 요청 시
     * INVALID_REGISTER_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void invalidRegisterTokenTest() {
        when(jwtProvider.validateRegisterToken("invalid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.INVALID);

        assertThatThrownBy(() -> authService.createUser("invalid-register-token", "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REGISTER_TOKEN));
    }

    /**
     * register session이 존재하지 않는 경우 테스트:
     * register token의 subject로 register session을 찾지 못하면
     * INVALID_REGISTER_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void registerSessionNotFoundTest() {
        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(100L);
        when(registerSessionRepository.findFirstBySocialAccountIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.createUser("valid-register-token", "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REGISTER_TOKEN));
    }

    /**
     * register token hash가 일치하지 않는 경우 테스트:
     * DB에 저장된 register token hash와 요청 token hash가 다르면
     * INVALID_REGISTER_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void registerTokenHashMismatchTest() {
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
                .thenReturn("different-hash");

        assertThatThrownBy(() -> authService.createUser("valid-register-token", "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REGISTER_TOKEN));
    }

    /**
     * 이미 사용된 register session 테스트:
     * consumed 처리된 register session으로 온보딩 요청 시
     * INVALID_REGISTER_TOKEN 예외가 발생하는지 확인
     */
    @Test
    void consumedRegisterSessionTest() {
        RegisterSession registerSession = new RegisterSession(
                1L,
                100L,
                "saved-hash",
                LocalDateTime.now()
        );
        registerSession.markConsumed(LocalDateTime.now());

        when(jwtProvider.validateRegisterToken("valid-register-token"))
                .thenReturn(JwtProvider.TokenValidationResult.VALID);
        when(jwtProvider.getSubjectAsLong("valid-register-token"))
                .thenReturn(100L);
        when(registerSessionRepository.findFirstBySocialAccountIdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(registerSession));
        when(tokenHasher.hash("valid-register-token"))
                .thenReturn("saved-hash");

        assertThatThrownBy(() -> authService.createUser("valid-register-token", "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REGISTER_TOKEN));
    }

    /**
     * 닉네임이 비어있는 경우 테스트:
     * 비어있는 닉네임으로 온보딩 요청 시
     * INVALID_INPUT_VALUE 예외와 EMPTY_NICKNAME 상세 오류가 발생하는지 확인
     */
    @Test
    void emptyNicknameTest() {
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

        assertThatThrownBy(() -> authService.createUser("valid-register-token", " "))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_INPUT_VALUE);
                    List<ErrorResponse.ErrorDetail> errors = exception.getErrors();
                    assertThat(errors.get(0).getField()).isEqualTo("nickname");
                    assertThat(errors.get(0).getCode()).isEqualTo(AuthErrorCode.EMPTY_NICKNAME.getCode());
                });
    }

    /**
     * 닉네임에 특수문자가 포함된 경우 테스트:
     * 특수문자가 포함된 닉네임으로 온보딩 요청 시
     * INVALID_INPUT_VALUE 예외와 SPECIAL_CHAR_NICKNAME 상세 오류가 발생하는지 확인
     */
    @Test
    void specialCharacterNicknameTest() {
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

        assertThatThrownBy(() -> authService.createUser("valid-register-token", "nickname!"))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_INPUT_VALUE);
                    List<ErrorResponse.ErrorDetail> errors = exception.getErrors();
                    assertThat(errors.get(0).getField()).isEqualTo("nickname");
                    assertThat(errors.get(0).getCode()).isEqualTo(AuthErrorCode.SPECIAL_CHAR_NICKNAME.getCode());
                });
    }

    /**
     * 닉네임 길이가 잘못된 경우 테스트:
     * 길이 조건을 만족하지 않는 닉네임으로 온보딩 요청 시
     * INVALID_INPUT_VALUE 예외와 NICKNAME_LENGTH_EXCEEDED 상세 오류가 발생하는지 확인
     */
    @Test
    void nicknameLengthExceededTest() {
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

        assertThatThrownBy(() -> authService.createUser("valid-register-token", "n"))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_INPUT_VALUE);
                    List<ErrorResponse.ErrorDetail> errors = exception.getErrors();
                    assertThat(errors.get(0).getField()).isEqualTo("nickname");
                    assertThat(errors.get(0).getCode()).isEqualTo(AuthErrorCode.NICKNAME_LENGTH_EXCEEDED.getCode());
                });
    }

    /**
     * social account를 찾지 못한 경우 테스트:
     * register session 검증까지 끝났는데 social account를 찾지 못하면
     * SERVER_ERROR 예외가 발생하는지 확인
     */
    @Test
    void socialAccountNotFoundTest() {
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
        when(tsidGenerator.nextId())
                .thenReturn(10L);
        when(socialAccountRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.createUser("valid-register-token", "nickname"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.SERVER_ERROR));
    }

    /**
     * 온보딩 성공 테스트:
     * - 유효한 register token과 register session, social account를 준비하고,
     * - (1) user가 저장되는지 확인
     * - (2) user stat이 함께 초기화되는지 확인
     * - (3) social account가 user와 연결되는지 확인
     * - (4) register session이 사용 처리되는지 확인
     * - (5) refresh token이 저장되는지 확인
     * - (6) access token과 refresh token이 반환되는지 확인
     */
    @Test
    void createUserSuccessTest() {
        RegisterSession registerSession = new RegisterSession(
                1L,
                100L,
                "saved-hash",
                LocalDateTime.now()
        );
        SocialAccount socialAccount = new SocialAccount(
                100L,
                OAuthProvider.GOOGLE,
                "google-auth-id",
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
        when(tsidGenerator.nextId())
                .thenReturn(10L, 20L);
        when(socialAccountRepository.findById(100L))
                .thenReturn(Optional.of(socialAccount));
        when(jwtProvider.createRefreshToken(10L, 20L))
                .thenReturn("new-refresh-token");
        when(tokenHasher.hash("new-refresh-token"))
                .thenReturn("hashed-refresh-token");
        when(jwtProvider.createAccessToken(10L))
                .thenReturn("new-access-token");

        OnboardingResult result = authService.createUser("valid-register-token", "nickname");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserHoguStat> userHoguStatCaptor = ArgumentCaptor.forClass(UserHoguStat.class);
        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(userRepository).save(userCaptor.capture());
        verify(userHoguStatRepository).save(userHoguStatCaptor.capture());
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        assertThat(userCaptor.getValue().getId()).isEqualTo(10L);
        assertThat(userCaptor.getValue().getNickname()).isEqualTo("nickname");
        assertThat(userHoguStatCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(userHoguStatCaptor.getValue().getHoguVoteCount()).isEqualTo(0);
        assertThat(userHoguStatCaptor.getValue().getTotalVoteCount()).isEqualTo(0);
        assertThat(userHoguStatCaptor.getValue().getHoguIndex()).isEqualTo(0);
        assertThat(socialAccount.getUserId()).isEqualTo(10L);
        assertThat(registerSession.isConsumed()).isTrue();
        assertThat(refreshTokenCaptor.getValue().getId()).isEqualTo(20L);
        assertThat(refreshTokenCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(refreshTokenCaptor.getValue().getTokenHash()).isEqualTo("hashed-refresh-token");
        assertThat(ReflectionTestUtils.getField(result, "accessToken")).isEqualTo("new-access-token");
        assertThat(ReflectionTestUtils.getField(result, "refreshToken")).isEqualTo("new-refresh-token");
    }
}
