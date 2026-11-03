package law.implatform.model.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class UserVO {
    /**
     * id
     */
    @NotNull(message = "用户id不能为空")
    private Long id;

    /**
     * 用户名
     */
    @NotEmpty(message = "用户名不能为空")
    @Length(max = 20, message = "用户名不能大于20字符")
    private String userName;

    /**
     * 用户昵称
     */
    @NotEmpty(message = "用户昵称不能为空")
    @Length(max = 20, message = "昵称不能大于20字符")
    private String nickName;

    /**
     * 性别
     */
    private Integer sex;

    /**
     * 用户类型 1:普通用户 2:审核账户
     */
    private Integer type;

    /**
     * 个性签名
     */
    @Length(max = 128, message = "个性签名不能大于128个字符")
    private String signature;

    /**
     * 头像
     */
    private String headImage;

    /**
     * 头像缩略图
     */
    private String headImageThumb;

    /**
     * 是否在线
     */
    private Boolean online;

    /**
     * 账号是否被封禁
     */
    private Boolean isBanned;

    /**
     * 被封禁原因
     */
    private String reason;
}
