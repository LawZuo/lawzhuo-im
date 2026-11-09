package law.imcommon.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import law.imcommon.serializer.DateToLongSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 私聊消息VO。
 *
 * <p>放在 im-common 而不是 im-platform：这条对象会被放进 IM 队列（{@code IMRecvInfo.data}），
 * 由 im-server 反序列化后原样下发给客户端。如果类只在 im-platform 中，im-server 的
 * {@code GenericJackson2JsonRedisSerializer} 会因为找不到 {@code @class} 对应的类型而抛
 * {@code SerializationException}。
 */
@Data
public class PrivateMessageVO {

    // 消息id
    private Long id;

    // 本地消息id
    private String localId;

    // 消息序列号，会话内连续递增
    private Long seqNo;

    // 发送者id
    private Long sendId;

    // 接收者id
    private Long recvId;

    // 发送内容
    private String content;

    // 消息内容类型 MessageType
    private Integer type;

    // 状态
    private Integer status;

    // 发送时间
    @JsonSerialize(using = DateToLongSerializer.class)
    private Date sendTime;

    // 是否删除
    private Boolean deleted;
}
