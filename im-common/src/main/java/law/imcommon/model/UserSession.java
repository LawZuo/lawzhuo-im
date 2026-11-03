package law.imcommon.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录用户会话信息，会作为 JWT 的 claims 内容。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSession extends IMSessionInfo {

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户昵称
     */
    private String nickName;
}
