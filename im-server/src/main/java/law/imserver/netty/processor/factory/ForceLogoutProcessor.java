package law.imserver.netty.processor.factory;

import cn.hutool.core.util.StrUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import law.imcommon.enums.IMCmdType;
import law.imcommon.model.IMForceLogoutData;
import law.imcommon.model.IMForceLogoutInfo;
import law.imcommon.model.IMSendInfo;
import law.imserver.constant.ChannelAttrKey;
import law.imserver.netty.UserChannelCtxMap;
import law.imserver.netty.processor.AbstractMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 强制退出处理器
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class ForceLogoutProcessor extends AbstractMessageProcessor<IMForceLogoutInfo> {

    @Override
    public void process(IMForceLogoutInfo info) {
        // 获取用户的channel
        ChannelHandlerContext context = UserChannelCtxMap.getChannelCtx(info.getUserId(), info.getTerminal());
        if (Objects.isNull(context)) {
            return;
        }
        // 获取设备
        AttributeKey<String> devIdAttr = AttributeKey.valueOf(ChannelAttrKey.DEVICE_ID);
        String devId = context.channel().attr(devIdAttr).get();

        if (StrUtil.isEmpty(info.getDevId()) || !info.getDevId().equals(devId)) {
            IMForceLogoutData data = new IMForceLogoutData();
            data.setType(info.getType());
            data.setReason(info.getReason());
            IMSendInfo<IMForceLogoutData> sendInfo = new IMSendInfo<>();
            sendInfo.setCmd(IMCmdType.FORCE_LOGOUT.code());
            sendInfo.setData(data);
            context.channel().writeAndFlush(sendInfo).addListener(ChannelFutureListener.CLOSE);
            log.info("强制下线,userId:{},终端:{},type:{}", info.getUserId(), info.getTerminal(), info.getType());
        }

    }
}
