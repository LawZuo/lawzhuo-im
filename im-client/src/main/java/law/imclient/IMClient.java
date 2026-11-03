package law.imclient;

import law.imclient.sender.IMSender;
import law.imcommon.enums.IMTerminalType;
import law.imcommon.model.IMPrivateMessage;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@AllArgsConstructor
public class IMClient {

    private final IMSender imSender;

    /**
     * 判断用户是否在线
     */
    public Boolean isOnline(Long userId){
        return imSender.isOnline(userId);
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户id
     * @param terminal 终端
     */
    public Boolean isOnline(Long userId,IMTerminalType terminal){
        return imSender.isOnline(userId,terminal);
    }

    /**
     * 发送私聊消息（发送结果通过MessageListener接收）
     */
    public<T> void sendPrivateMessage(IMPrivateMessage<T> message){
        imSender.sendPrivateMessage(message);
    }

    /**
     * 判断多个用户是否在线
     *
     * @param userIds 用户id列表
     * @return 在线的用户终端
     */
    public Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds){
        return imSender.getOnlineTerminal(userIds);
    }
}
