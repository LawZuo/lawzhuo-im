package law.implatform.util;

import law.imcommon.model.UserSession;
import law.implatform.enums.ResultCode;
import law.implatform.exception.GlobalException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * 当前登录用户工具。
 *
 * <p>唯一身份来源是 OAuth2 资源服务器解析出的 {@link JwtAuthenticationToken}：
 * {@code sub} 是 userId，其余字段（terminal / userName / nickName）从自定义 claim 还原，
 * 不需要查库，也不依赖 request 上下文。
 */
public final class SecurityUtils {

    private static final String CLAIM_TERMINAL = "terminal";
    private static final String CLAIM_USER_NAME = "userName";
    private static final String CLAIM_NICK_NAME = "nickName";

    private SecurityUtils() {
    }

    /**
     * 当前登录用户 id
     */
    public static Long getUserId() {
        return getSession().getUserId();
    }

    /**
     * 当前登录用户会话（userId / terminal / userName / nickName）
     *
     * @throws GlobalException 未登录，或 token 缺少 terminal
     */
    public static UserSession getSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new GlobalException(ResultCode.NO_LOGIN);
        }

        Jwt jwt = token.getToken();
        UserSession session = new UserSession();
        session.setUserId(Long.valueOf(jwt.getSubject()));
        session.setUserName(jwt.getClaimAsString(CLAIM_USER_NAME));
        session.setNickName(jwt.getClaimAsString(CLAIM_NICK_NAME));

        Object terminal = jwt.getClaim(CLAIM_TERMINAL);
        if (!(terminal instanceof Number number)) {
            // terminal 参与消息路由（im:user:server_id:{userId}:{terminal}），缺了会导致消息投递不到
            throw new GlobalException(ResultCode.INVALID_TOKEN, "登录信息缺少终端类型，请重新登录");
        }
        session.setTerminal(number.intValue());
        return session;
    }
}
