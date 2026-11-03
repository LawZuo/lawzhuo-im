package law.implatform.model.vo;

import lombok.Data;

/**
 * 用户登录VO
 */
@Data
public class LoginVO {

    /**
     * 每次请求都必须在header中携带accessToken
     */
    private String accessToken;

    /**
     * accessToken过期时间(秒)
     */
    private Integer accessTokenExpiresIn;

    /**
     * accessToken过期后，通过refreshToken换取新的token
     */
    private String refreshToken;

    /**
     * refreshToken过期时间(秒)
     */
    private Integer refreshTokenExpiresIn;
}
