package law.implatform.model.vo;

import lombok.Data;

/**
 * 用户在线状态VO
 */
@Data
public class UserOnlineVO {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 终端类型
     */
    private Integer terminal;

    /**
     * 是否在线
     */
    private Boolean online;
}
