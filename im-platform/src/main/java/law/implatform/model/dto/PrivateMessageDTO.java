package law.implatform.model.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@TableName("im_private_message")
public class PrivateMessageDTO {

    /**
     * 本地消息id,前端通过雪花算法生成
     */
    @NotEmpty(message = "本地消息id不可为空")
    private String localId;

    /**
     * 接收用户id
     */
    @NotNull(message = "接收用户id不可为空")
    private Long recvId;

    /**
     * 发送内容
     */
    @Length(max = 1024, message = "内容长度不得大于1024")
    @NotEmpty(message = "发送内容不可为空")
    private String content;

    /**
     * 消息类型 0:文字 1:图片 2:文件 3:语音 4:视频
     */
    @NotNull(message = "消息类型不可为空")
    private Integer type;
}
