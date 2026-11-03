package law.implatform.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改好友（备注、免打扰、置顶）
 */
@Data
public class FriendUpdateDTO {

    @NotNull(message = "好友id不能为空")
    private Long friendId;

    /**
     * 好友备注，为空则不修改
     */
    private String friendNickName;

    /**
     * 是否免打扰，为空则不修改
     */
    private Boolean isDnd;

    /**
     * 是否置顶，为空则不修改
     */
    private Boolean isTop;
}
