package law.imclient;

import law.imclient.sender.IMSender;
import law.imcommon.enums.IMTerminalType;
import law.imcommon.model.IMPrivateMessage;
import law.imcommon.model.IMSystemMessage;
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
     * 判断多个用户是否在线
     *
     * @param userIds 用户id列表
     * @return 在线的用户终端
     */
    public Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds){
        return imSender.getOnlineTerminal(userIds);
    }

    /**
     * 强制用户所有在线终端下线
     *
     * @param userId 用户id
     * @param type   下线类型，见 IMForceLogoutType
     * @param reason 原因说明
     */
    public void forceLogout(Long userId, Integer type, String reason){
        imSender.forceLogout(userId, type, reason);
    }

    /**
     * 发送私聊消息（发送结果通过MessageListener接收）
     */
    public<T> void sendPrivateMessage(IMPrivateMessage<T> message){
        imSender.sendPrivateMessage(message);
    }

    /**
     * 发送系统消息（发送结果通过MessageListener接收）
     *
     * @param message 私有消息
     */
    public<T> void sendSystemMessage(IMSystemMessage<T> message){
        imSender.sendSystemMessage(message);
    }
}
