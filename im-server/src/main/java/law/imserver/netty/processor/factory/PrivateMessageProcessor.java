package law.imserver.netty.processor.factory;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import law.imcommon.enums.IMCmdType;
import law.imcommon.model.IMRecvInfo;
import law.imcommon.model.IMSendInfo;
import law.imcommon.model.IMUserInfo;
import law.imserver.netty.UserChannelCtxMap;
import law.imserver.netty.processor.AbstractMessageProcessor;
import law.imserver.netty.processor.PushTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 私聊处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessageProcessor extends AbstractMessageProcessor<IMRecvInfo> {

    @Override
    public void process(IMRecvInfo recvInfo) {
        // 获取发送方数据
        IMUserInfo sender = recvInfo.getSender();
        List<PushTask> pushTasks = new ArrayList<>(recvInfo.getReceivers().size());

        // 遍历接收方用户列表
        for (IMUserInfo receiver: recvInfo.getReceivers()) {
            log.info("接收到私聊消息，发送者:{},接收者:{}，内容:{}", sender.getId(), receiver.getId(),
                    recvInfo.getData());

            // 根据用户id和设备获取通道
            ChannelHandlerContext channelCtx =
                    UserChannelCtxMap.getChannelCtx(receiver.getId(), receiver.getTerminal());

            if (!Objects.isNull(channelCtx)) {
                // ws消息格式
                IMSendInfo<Object> sendInfo = new IMSendInfo<>();
                sendInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
                sendInfo.setData(recvInfo.getData());
                pushTasks.add(new PushTask(receiver, channelCtx.channel().writeAndFlush(sendInfo)));
            } else {
                log.error("未找到channel，发送者:{},接收者:{}，内容:{}", sender.getId(), receiver.getId(),
                        recvInfo.getData());
            }
        }

        // 发送

        for (PushTask task : pushTasks) {
            task.getFuture().addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    log.error("推送失败, 接收者:{}", task.getReceiver().getId(), f.cause());
                }
            });
        }
    }
}
