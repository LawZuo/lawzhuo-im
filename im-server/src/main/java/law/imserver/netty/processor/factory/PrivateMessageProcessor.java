package law.imserver.netty.processor.factory;

import law.imcommon.model.IMRecvInfo;
import law.imcommon.model.IMUserInfo;
import law.imserver.netty.processor.AbstractMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 私聊处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessageProcessor extends AbstractMessageProcessor<IMRecvInfo> {

    @Override
    public void process(IMRecvInfo recvInfo) {
        // 获取发送方数据
        IMUserInfo sender = recvInfo.getSender();

        // 遍历接收方用户列表
        for (IMUserInfo receiver: recvInfo.getReceivers()) {
            log.info("接收到私聊消息，发送者:{},接收者:{}，内容:{}", sender.getId(), receiver.getId(),
                    recvInfo.getData());
        }
    }
}
