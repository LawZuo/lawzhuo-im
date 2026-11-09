package law.implatform.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 处理好友申请
 */

public class FriendRequestBTO {

    @NotNull(message = "好友id不能为空")
    private Long friendId;

    /**
     * 处理结果
     * 1通过 2拒绝
     */
    private Integer status;

}
