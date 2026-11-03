package law.imcommon.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import law.imcommon.model.UserSession;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.util.StringUtils;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

/**
 * JWT 签发与校验（RS256）。
 *
 * <p>签发方（im-platform）需要私钥；校验方（im-server）只需要公钥，
 * 因此服务端即使配置泄露也签不出 token。
 *
 * <p>access / refresh token 用同一个密钥对签发，靠 {@code token_type} claim 区分，
 * 校验时会强制比对类型，避免 refresh token 被当成 access token 使用。
 */
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_TYPE = "token_type";
    private static final String CLAIM_TERMINAL = "terminal";
    private static final String CLAIM_USER_NAME = "userName";
    private static final String CLAIM_NICK_NAME = "nickName";

    private final JwtProperties properties;
    private final NimbusJwtDecoder accessDecoder;
    private final NimbusJwtDecoder refreshDecoder;
    private final JwtEncoder encoder;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        if (!StringUtils.hasText(properties.getPublicKey())) {
            throw new IllegalStateException("缺少 jwt.public-key 配置（base64(X.509 DER)）");
        }
        RSAPublicKey publicKey = RsaKeys.parsePublicKey(properties.getPublicKey());
        this.accessDecoder = buildDecoder(publicKey, TYPE_ACCESS);
        this.refreshDecoder = buildDecoder(publicKey, TYPE_REFRESH);
        // 校验方（im-server）没有私钥，此时不创建 encoder
        this.encoder = StringUtils.hasText(properties.getPrivateKey())
                ? new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(
                        new RSAKey.Builder(publicKey)
                                .privateKey(RsaKeys.parsePrivateKey(properties.getPrivateKey()))
                                .build())))
                : null;
    }

    /**
     * 是否具备签发能力（只有配置了私钥的 im-platform 才为 true）
     */
    public boolean canSign() {
        return encoder != null;
    }

    public String createAccessToken(UserSession session) {
        return create(session, properties.getAccessToken().getExpireIn(), TYPE_ACCESS);
    }

    public String createRefreshToken(UserSession session) {
        return create(session, properties.getRefreshToken().getExpireIn(), TYPE_REFRESH);
    }

    /**
     * 校验并解析 access token。
     *
     * @throws JwtException 签名不对、已过期、issuer 不匹配、token 类型不对
     */
    public UserSession parseAccessToken(String token) {
        return toSession(accessDecoder.decode(token));
    }

    /**
     * 校验并解析 refresh token。
     */
    public UserSession parseRefreshToken(String token) {
        return toSession(refreshDecoder.decode(token));
    }

    /**
     * 供 Spring Security 资源服务器使用的解码器（只接受 access token）
     */
    public JwtDecoder getAccessTokenDecoder() {
        return accessDecoder;
    }

    private String create(UserSession session, long expireIn, String type) {
        if (encoder == null) {
            throw new IllegalStateException("当前服务未配置 jwt.private-key，无法签发 token");
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(session.getUserId()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expireIn))
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_TERMINAL, session.getTerminal())
                .claim(CLAIM_USER_NAME, session.getUserName())
                .claim(CLAIM_NICK_NAME, session.getNickName())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private UserSession toSession(Jwt jwt) {
        UserSession session = new UserSession();
        session.setUserId(Long.valueOf(jwt.getSubject()));
        Object terminal = jwt.getClaim(CLAIM_TERMINAL);
        if (terminal instanceof Number number) {
            session.setTerminal(number.intValue());
        }
        session.setUserName(jwt.getClaimAsString(CLAIM_USER_NAME));
        session.setNickName(jwt.getClaimAsString(CLAIM_NICK_NAME));
        return session;
    }

    private NimbusJwtDecoder buildDecoder(RSAPublicKey publicKey, String expectedType) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        OAuth2TokenValidator<Jwt> typeValidator = jwt -> expectedType.equals(jwt.getClaimAsString(CLAIM_TYPE))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "token 类型不正确，需要 " + expectedType, null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.getIssuer()),
                typeValidator));
        return decoder;
    }
}
