package law.imserver.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import law.imcommon.enums.IMCmdType;
import law.imcommon.model.IMSendInfo;
import law.imserver.netty.processor.AbstractMessageProcessor;
import law.imserver.netty.processor.ProcessorFactory;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.processing.Processor;

/**
 * WebSocket 长连接下 文本帧的处理器 实现浏览器发送文本回写 浏览器连接状态监控
 */

@Slf4j
public class IMChannelHandler extends SimpleChannelInboundHandler<IMSendInfo> {

    /**
     * 读取到消息后进行处理
     * @param ctx channel上下文
     * @param sendInfo 发送消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMSendInfo sendInfo) {
        // 创建处理器进行处理
        AbstractMessageProcessor processor = ProcessorFactory.createProcessor(IMCmdType.fromCode(sendInfo.getCmd()));
        processor.process(ctx, processor.transForm(sendInfo.getData()));
    }
}
