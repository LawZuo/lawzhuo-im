package law.imclient.task;

import law.imclient.listener.MessageListenerMulticaster;
import law.imcommon.contant.IMRedisKey;
import law.imcommon.enums.IMListenerType;
import law.imcommon.model.IMBatchSendResult;
import law.imcommon.mq.RedisMQListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@RedisMQListener(queue = IMRedisKey.IM_RESULT_PRIVATE_QUEUE, batchSize = 100)
public class PrivateMessageResultResultTask extends AbstractMessageResultTask<IMBatchSendResult> {

    private final MessageListenerMulticaster listenerMulticaster;

    @Override
    public void onMessage(List<IMBatchSendResult> batchResults) {
        listenerMulticaster.multicast(IMListenerType.PRIVATE_MESSAGE, batchResults);
    }

}
