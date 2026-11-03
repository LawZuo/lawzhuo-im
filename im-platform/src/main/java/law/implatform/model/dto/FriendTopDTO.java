package law.implatform.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendTopDTO {
    @NotNull(message = "好友id不可为空")
    private Long friendId;

    @NotNull(message = "置顶状态不可为空")
    private Boolean isTop;
}
