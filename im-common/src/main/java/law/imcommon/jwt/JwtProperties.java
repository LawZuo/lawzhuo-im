package law.imcommon.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（RS256）。
 *
 * <p>私钥只有签发方（im-platform）持有；校验方（im-server）只需要公钥。
 * 两个值都是 base64 编码的 DER：私钥是 PKCS#8，公钥是 X.509。
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 签发者标识，写入 iss，校验时比对
     */
    private String issuer = "im-platform";

    /**
     * base64(X.509 DER) 公钥，校验方必需
     */
    private String publicKey;

    /**
     * base64(PKCS#8 DER) 私钥，只有签发方需要
     */
    private String privateKey;

    private Token accessToken = new Token();

    private Token refreshToken = new Token();

    @Data
    public static class Token {
        /**
         * 有效期（秒）
         */
        private long expireIn;
    }
}
