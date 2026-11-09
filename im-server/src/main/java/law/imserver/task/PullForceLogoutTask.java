package law.imserver.task;

import law.imcommon.contant.IMRedisKey;
import law.imcommon.enums.IMCmdType;
import law.imcommon.model.IMForceLogoutInfo;
import law.imcommon.mq.RedisMQListener;
import law.imserver.netty.processor.AbstractMessageProcessor;
import law.imserver.netty.processor.ProcessorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 强制退出登录
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RedisMQListener(queue = IMRedisKey.IM_USER_FORCE_LOGOUT_QUEUE)
public class PullForceLogoutTask extends AbstractPullMessageTask<IMForceLogoutInfo> {

    @Override
    public void onMessage(IMForceLogoutInfo logoutInfo) {
        log.info("【im-server】PullForceLogoutTask：收到强踢通知");
        AbstractMessageProcessor processor = ProcessorFactory.createProcessor(IMCmdType.FORCE_LOGOUT);
        processor.process(logoutInfo);
    }
}
