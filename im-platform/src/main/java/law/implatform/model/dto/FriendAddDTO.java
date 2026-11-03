package law.implatform.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加好友
 */
@Data
public class FriendAddDTO {

    @NotNull(message = "好友id不能为空")
    private Long friendId;

    /**
     * 好友备注（不填则用对方的昵称）
     */
    private String friendNickName;
}
