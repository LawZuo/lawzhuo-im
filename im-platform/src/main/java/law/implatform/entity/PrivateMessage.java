package law.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 私聊消息
 */
@Data
@TableName("im_private_message")
public class PrivateMessage {

    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 业务id，由前端生成（用于消息去重/重发）
     */
    @TableField("local_id")
    private String localId;

    /**
     * 序列号，单个会话内连续递增
     */
    @TableField("seq_no")
    private Long seqNo;

    /**
     * 发送用户id
     */
    @TableField("send_id")
    private Long sendId;

    /**
     * 接收用户id
     */
    @TableField("recv_id")
    private Long recvId;

    /**
     * 会话key，格式：userId1_userId2
     */
    @TableField("conv_key")
    private String convKey;

    /**
     * 发送内容
     */
    @TableField("content")
    private String content;

    /**
     * 消息类型 0:文字 1:图片 2:文件 3:语音 4:视频 21:提示
     */
    @TableField("type")
    private Integer type;

    /**
     * 状态 0:未读 1:已发送 2:撤回 3:已读
     */
    @TableField("status")
    private Integer status;

    /**
     * 发送时间
     */
    @TableField("send_time")
    private Date sendTime;
}
