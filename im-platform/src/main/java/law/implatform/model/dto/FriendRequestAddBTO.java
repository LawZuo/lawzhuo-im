package law.implatform.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendRequestAddBTO {
    @NotNull(message = "好友id不能为空")
    private Long toUserId;

    /**
     * 申请留言
     */
    private String message;
}
