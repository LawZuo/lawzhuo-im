package law.implatform.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 好友信息VO
 */

@Data
public class FriendVO {

    /**
     * 好友id
     */
    @NotNull(message = "好友id不可为空")
    private Long id;

    /**
     * 好友昵称
     */
    @NotNull(message = "好友昵称不可为空")
    private String nickName;


    /**
     * 好友头像
     */
    private String headImage;

    /**
     * 是否开启免打扰
     */
    private Boolean isDnd;

    /**
     * 是否置顶会话
     */
    private Boolean isTop;

    /**
     * 是否已删除
     */
    private Boolean deleted;

    /**
     * 版本号
     */
    private Long version;
}
