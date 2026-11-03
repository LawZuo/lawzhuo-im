package law.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;

@Data
@TableName("im_user")
public class User {
    /**
     * 用户id
     */
    @TableId
    private Long id;

    /**
     * 用户名
     */
    @TableField("user_name")
    private String userName;

    /**
     * 用户昵称
     */
    @TableField("nick_name")
    private String nickName;

    /**
     * 用户头像
     */
    @TableField("head_image")
    private String headImage;

    /**
     * 用户头像缩略图
     */
    @TableField("head_image_thumb")
    private String headImageThumb;

    /**
     * 密码(明文)
     */
    @TableField(select = false)
    private String password;

    /**
     * 性别 0:男 1::女
     */
    @TableField("sex")
    private Integer sex;

    /**
     * 个性签名
     */
    @TableField("signature")
    private String signature;

    /**
     * 账号是否被封禁
     */
    @TableField("is_banned")
    private Boolean isBanned;

    /**
     * 账号被封禁原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private Date lastLoginTime;

    /**
     * 创建时间(注册时间)
     */
    @TableField("created_time")
    private Date createdTime;

    /**
     *  账号类型 1:普通用户 2:wx小程序审核账户
     */
    @TableField("type")
    private Integer type;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer is_deleted;
}
