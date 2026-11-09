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

    /**
     * 用户事件由 im-server 直接写入 {@code im:user:event}，不带应用名后缀，
     * 这里必须和生产者保持一致；否则事件会一直堆在 {@code im:user:event} 里没人消费。
     */
    @Override
    public String generateKey() {
        return IMRedisKey.IM_USER_EVENT_QUEUE;
    }

    @Override
    public void onMessage(List<IMUserEvent> events) {
        log.info("【im-client】UserEventTask, 收到用户事件，时间类型：eventType");
        listenerMulticaster.multicast(events);
    }
}
