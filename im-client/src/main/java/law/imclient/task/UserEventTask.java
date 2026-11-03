package law.imclient.task;

import law.imclient.listener.EventListenerMulticaster;
import law.imcommon.contant.IMRedisKey;
import law.imcommon.model.IMUserEvent;
import law.imcommon.mq.RedisMQListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@RedisMQListener(queue = IMRedisKey.IM_USER_EVENT_QUEUE, batchSize = 100)
public class UserEventTask extends AbstractMessageResultTask<IMUserEvent> {

    private final EventListenerMulticaster listenerMulticaster;

    @Override
    public void onMessage(List<IMUserEvent> events) {
        log.info("【im-client】UserEventTask, 收到用户事件，时间类型：eventType");
        listenerMulticaster.multicast(events);
    }
}
