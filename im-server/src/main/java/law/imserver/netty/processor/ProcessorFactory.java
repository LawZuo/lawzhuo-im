package law.imserver.netty.processor;

import law.imcommon.enums.IMCmdType;
import law.imserver.netty.processor.factory.HeartbeatProcessor;
import law.imserver.netty.processor.factory.LoginMessageProcessor;
import law.imserver.netty.processor.factory.PrivateMessageProcessor;
import law.imserver.util.SpringContextHolder;

/**
 * 处理器工厂
 */

public class ProcessorFactory {

    public static AbstractMessageProcessor createProcessor(IMCmdType cmd) {

        return switch (cmd) {
            case LOGIN -> SpringContextHolder.getApplicationContext().getBean(LoginMessageProcessor.class);
            case HEART_BEAT -> SpringContextHolder.getApplicationContext().getBean(HeartbeatProcessor.class);
            case PRIVATE_MESSAGE -> SpringContextHolder.getApplicationContext().getBean(PrivateMessageProcessor.class);
            default -> null;
        };
    }
}
