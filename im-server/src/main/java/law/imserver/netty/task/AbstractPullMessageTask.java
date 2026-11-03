package law.imserver.netty.task;

import law.imcommon.mq.RedisMQConsumer;
import law.imserver.netty.IMServerGroup;
import org.springframework.beans.factory.annotation.Autowired;

public class AbstractPullMessageTask<T> extends RedisMQConsumer<T> {

    @Autowired
    private IMServerGroup serverGroup;

    @Override
    public String generateKey() {
        return String.join(":",  super.generateKey(), IMServerGroup.serverId + "");
    }

    @Override
    public Boolean isReady() {
        return serverGroup.isReady();
    }
}
