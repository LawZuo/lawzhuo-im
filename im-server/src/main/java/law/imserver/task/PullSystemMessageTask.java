package law.imserver.task;

import law.imcommon.contant.IMRedisKey;
import law.imcommon.enums.IMCmdType;
import law.imcommon.model.IMRecvInfo;
import law.imcommon.mq.RedisMQListener;
import law.imserver.netty.processor.AbstractMessageProcessor;
import law.imserver.netty.processor.ProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RedisMQListener(queue = IMRedisKey.IM_MESSAGE_SYSTEM_QUEUE,batchSize = 10)
public class PullSystemMessageTask extends AbstractPullMessageTask<IMRecvInfo> {

    @Override
    public void onMessage(IMRecvInfo recvInfo) {
        AbstractMessageProcessor processor = ProcessorFactory.createProcessor(IMCmdType.SYSTEM_MESSAGE);
        processor.process(recvInfo);
    }

}
