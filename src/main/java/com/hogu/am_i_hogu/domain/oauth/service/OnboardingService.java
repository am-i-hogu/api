package com.hogu.am_i_hogu.domain.oauth.service;

import com.hogu.am_i_hogu.common.exception.CommonErrorCode;
import com.hogu.am_i_hogu.common.exception.CustomException;
import com.hogu.am_i_hogu.common.exception.ErrorResponse;
import com.hogu.am_i_hogu.common.security.JwtProvider;
import com.hogu.am_i_hogu.common.security.TokenHasher;
import com.hogu.am_i_hogu.common.util.TsidGenerator;
import com.hogu.am_i_hogu.domain.oauth.domain.RefreshToken;
import com.hogu.am_i_hogu.domain.oauth.domain.RegisterSession;
import com.hogu.am_i_hogu.domain.oauth.domain.SocialAccount;
import com.hogu.am_i_hogu.domain.oauth.domain.User;
import com.hogu.am_i_hogu.domain.oauth.dto.response.OnboardingResult;
import com.hogu.am_i_hogu.domain.oauth.exception.OAuthErrorCode;
import com.hogu.am_i_hogu.domain.oauth.repository.RefreshTokenRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.RegisterSessionRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.SocialAccountRepository;
import com.hogu.am_i_hogu.domain.oauth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OnboardingService {

    private final JwtProvider jwtProvider;
    private final RegisterSessionRepository registerSessionRepository;
    private final UserRepository userRepository;
    private final TokenHasher tokenHasher;
    private final TsidGenerator tsidGenerator;
    private final SocialAccountRepository socialAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public OnboardingService(
            JwtProvider jwtProvider,
            RegisterSessionRepository registerSessionRepository,
            UserRepository userRepository,
            TokenHasher tokenHasher,
            TsidGenerator tsidGenerator,
            SocialAccountRepository socialAccountRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.jwtProvider = jwtProvider;
        this.registerSessionRepository = registerSessionRepository;
        this.userRepository = userRepository;
        this.tokenHasher = tokenHasher;
        this.tsidGenerator = tsidGenerator;
        this.socialAccountRepository = socialAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public OnboardingResult createUser(String authorizationHeader, String nickname) {
        String registerToken = loadAndValidateRegisterToken(authorizationHeader);
        RegisterSession registerSession = loadAndValidateRegisterSession(registerToken);
        validateNickname(nickname);

        LocalDateTime createdAt = LocalDateTime.now();
        Long userId = saveUserAndLinkSocialAccount(nickname, registerSession, createdAt);

        return issueLoginTokens(userId, createdAt);
    }

    // register token 추출 후 검증 (register token 타입이 아닌지 / 비어있는지 / 만료되었는지)
    private String loadAndValidateRegisterToken(String authorizationHeader) {
        String registerToken = getRegisterToken(authorizationHeader);

        JwtProvider.TokenValidationResult validationResult = jwtProvider.validateRegisterToken(registerToken);

        if (validationResult == JwtProvider.TokenValidationResult.EMPTY) {                      // register token이 비어있는 경우
            throw new CustomException(OAuthErrorCode.EMPTY_REGISTER_TOKEN);
        } else if (validationResult == JwtProvider.TokenValidationResult.EXPIRED) {             // register token이 만료된 경우
            throw new CustomException(OAuthErrorCode.REGISTER_TOKEN_EXPIRED);
        } else if (validationResult == JwtProvider.TokenValidationResult.INVALID) {             // register token이 잘못된 값인 경우
            throw new CustomException(OAuthErrorCode.INVALID_REGISTER_TOKEN);
        }

        return registerToken;
    }

    // authorization header에서 register token 추출
    private String getRegisterToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {     // 헤더가 비어있는 경우
            return null;
        }
        if (!authorizationHeader.startsWith("Bearer ")) {                       // "Bearer" 타입으로 들어오지 않은 경우
            return authorizationHeader;
        }

        String registerToken = authorizationHeader.substring(7);
        return registerToken.isBlank() ? null : registerToken;
    }

    // register session 검색 및 검증 (DB에 저장된 값과 일치하는지 / 사용되었는지)
    private RegisterSession loadAndValidateRegisterSession(String registerToken) {
        Long socialAccountId = jwtProvider.getSubjectAsLong(registerToken);
        RegisterSession registerSession = registerSessionRepository.findBySocialAccountId(socialAccountId)
                .orElseThrow(()-> new CustomException(OAuthErrorCode.INVALID_REGISTER_TOKEN));

        if (!registerSession.getRegisterTokenHash().equals(tokenHasher.hash(registerToken))) {  // DB의 register token hash 값과 일치하지 않는 경우
            throw new CustomException(OAuthErrorCode.INVALID_REGISTER_TOKEN);
        }
        if (registerSession.isConsumed()) {                                                     // register session이 이미 사용된 경우
            throw new CustomException(OAuthErrorCode.INVALID_REGISTER_TOKEN);
        }

        return registerSession;
    }

    // 닉네임 검증
    private void validateNickname(String nickname) {
        List<ErrorResponse.ErrorDetail> errors = new ArrayList<>();

        if (nickname == null || nickname.isBlank()) {               // 닉네임이 비어있는 경우
            errors.add(new ErrorResponse.ErrorDetail(
                    "nickname",
                    OAuthErrorCode.EMPTY_NICKNAME.getCode()
            ));
            throw new CustomException(OAuthErrorCode.INVALID_INPUT_VALUE, errors);
        }

        if (!nickname.matches("^[가-힣a-zA-Z0-9]+$")) {        // 닉네임에 특수문자가 포함된 경우
            errors.add(new ErrorResponse.ErrorDetail(
                    "nickname",
                    OAuthErrorCode.SPECIAL_CHAR_NICKNAME.getCode()
            ));
        }
        if (nickname.length() < 2 || nickname.length() > 20) {    // 닉네임 길이 위반한 경우
            errors.add(new ErrorResponse.ErrorDetail(
                    "nickname",
                    OAuthErrorCode.NICKNAME_LENGTH_EXCEEDED.getCode()
            ));
        }

        if (!errors.isEmpty()) {
            throw new CustomException(OAuthErrorCode.INVALID_INPUT_VALUE, errors);
        }
    }

    // 신규 유저 생성 후 register session 사용 처리 및 social account 연결
    private long saveUserAndLinkSocialAccount(String nickname, RegisterSession registerSession, LocalDateTime createdAt) {
        Long userId = tsidGenerator.nextId();
        User user = new User(
                userId,
                nickname,
                false,
                createdAt
        );
        userRepository.save(user);

        SocialAccount socialAccount = socialAccountRepository.findById(registerSession.getSocialAccountId())
                .orElseThrow(()-> new CustomException(CommonErrorCode.SERVER_ERROR));
        socialAccount.linkToUser(userId, createdAt);
        registerSession.markConsumed(createdAt);

        return userId;
    }

    // access token, refresh token 발급
    private OnboardingResult issueLoginTokens(Long userId, LocalDateTime createdAt) {
        Long refreshTokenId = tsidGenerator.nextId();
        String refreshToken = jwtProvider.createRefreshToken(userId, refreshTokenId);
        RefreshToken refreshTokenEntity = new RefreshToken(
                refreshTokenId,
                userId,
                tokenHasher.hash(refreshToken),
                createdAt
        );
        refreshTokenRepository.save(refreshTokenEntity);

        String accessToken = jwtProvider.createAccessToken(userId);

        return new OnboardingResult(accessToken, refreshToken);
    }
}
