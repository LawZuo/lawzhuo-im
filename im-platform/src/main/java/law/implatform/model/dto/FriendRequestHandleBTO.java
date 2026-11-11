package law.implatform.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理好友申请
 */
@Data
public class FriendRequestHandleBTO {

    @NotNull(message = "好友id不能为空")
    private Long fromUserId;

    /**
     * 处理结果
     * 1通过 2拒绝
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

}
