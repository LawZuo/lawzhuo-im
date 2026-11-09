package law.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 好友申请
 */
@Data
@TableName("im_friend_request")
public class FriendRequest {
    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 申请人
     */
    private Long fromUserId;

    /**
     * 被申请人
     */
    private Long toUserId;

    /**
     * 申请留言
     */
    private String message;

    /**
     * 申请状态
     */
    private Integer status;

    /**
     * 申请时间
     */
    private Date applyTime;

    /**
     * 处理时间
     */
    private Date handleTime;

    /**
     * 版本号
     */
    private Long version;
}
