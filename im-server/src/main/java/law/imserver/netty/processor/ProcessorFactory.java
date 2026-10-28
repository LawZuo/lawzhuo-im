package law.imserver.netty.processor;

import law.imcommon.enums.IMCmdType;
import law.imserver.netty.processor.factory.PrivateMessageProcessor;
import law.imserver.util.SpringContextHolder;

/**
 * 处理器工厂
 */

public class ProcessorFactory {

    public static AbstractMessageProcessor createProcessor(IMCmdType cmd) {

        return switch (cmd) {
            case PRIVATE_MESSAGE -> SpringContextHolder.getApplicationContext().getBean(PrivateMessageProcessor.class);
            default -> null;
        };
    }
}
