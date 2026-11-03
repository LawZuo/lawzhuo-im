package law.implatform.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import law.imcommon.serializer.DateToLongSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 私聊消息VO
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
