package law.imserver.netty.task;

import law.imcommon.contant.IMRedisKey;
import law.imcommon.enums.IMCmdType;
import law.imcommon.model.IMRecvInfo;
import law.imcommon.mq.RedisMQListener;
import law.imserver.netty.processor.AbstractMessageProcessor;
import law.imserver.netty.processor.ProcessorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 推送私聊消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RedisMQListener(queue = IMRedisKey.IM_MESSAGE_PRIVATE_QUEUE, batchSize = 100, period = 10)
public class PullPrivateMessageTask extends AbstractPullMessageTask<IMRecvInfo> {

    @Override
    public void onMessage(IMRecvInfo recvInfo) {
        log.info("收到要推送的私聊消息: {}", recvInfo.toString());
        AbstractMessageProcessor processor = ProcessorFactory.createProcessor(IMCmdType.PRIVATE_MESSAGE);
        processor.process(recvInfo);
    }
}
