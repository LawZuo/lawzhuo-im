package law.imcommon.jwt;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 把 base64 编码的 DER 密钥还原成 RSA 密钥对象。
 */
public final class RsaKeys {

    private RsaKeys() {
    }

    /**
     * @param base64 base64(X.509 DER)
     */
    public static RSAPublicKey parsePublicKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new IllegalStateException("JWT 公钥解析失败，请检查 jwt.public-key 是否为 base64(X.509 DER)", e);
        }
    }

    /**
     * @param base64 base64(PKCS#8 DER)
     */
    public static RSAPrivateKey parsePrivateKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new IllegalStateException("JWT 私钥解析失败，请检查 jwt.private-key 是否为 base64(PKCS#8 DER)", e);
        }
    }
}
