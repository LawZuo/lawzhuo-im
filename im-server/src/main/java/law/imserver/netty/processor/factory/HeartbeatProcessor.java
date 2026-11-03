package law.imserver.netty.processor.factory;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import law.imcommon.contant.IMConstant;
import law.imcommon.contant.IMRedisKey;
import law.imcommon.enums.IMCmdType;
import law.imcommon.model.IMHeartbeatInfo;
import law.imcommon.model.IMSendInfo;
import law.imcommon.mq.RedisMQTemplate;
import law.imserver.constant.ChannelAttrKey;
import law.imserver.netty.processor.AbstractMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * 心跳
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatProcessor extends AbstractMessageProcessor<IMHeartbeatInfo> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final RedisMQTemplate redisMQTemplate;

    @Override
    public void process(ChannelHandlerContext ctx, IMHeartbeatInfo beatInfo) {
        // 响应ws
        IMSendInfo<Object> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(IMCmdType.HEART_BEAT.code());
        ctx.channel().writeAndFlush(sendInfo);

        // 设置属性
        AttributeKey<Long> heartBeatAttr = AttributeKey.valueOf(ChannelAttrKey.HEARTBEAT_TIMES);
        Long heartbeatTimes = ctx.channel().attr(heartBeatAttr).get(); // 获取心跳次数
        ctx.channel().attr(heartBeatAttr).set(++heartbeatTimes); // 心跳+1
        log.info("【im-server】心跳，当前心跳次数：{}", ctx.channel().attr(heartBeatAttr).get());

        if (heartbeatTimes % 10 == 0) {
            // 每心跳10次，用户在线状态续一次命
            AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
            Long userId = ctx.channel().attr(userIdAttr).get(); // 从当前通道中获取userid
            AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
            Integer terminal = ctx.channel().attr(terminalAttr).get(); // 从当前通道中获取终端类型
            // 根据userId 终端类型获取用户的channel key，更新过期时间
            String key = IMRedisKey.userServerIdKey(userId, terminal);
            redisMQTemplate.expire(key, IMConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
        }
        AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
        Long userId = ctx.channel().attr(userIdAttr).get();
        log.debug("心跳,userId:{},{}",userId,ctx.channel().id().asLongText());
    }

    /**
     * 解码器把 data 解析成了 LinkedHashMap，这里转成具体类型，
     * 否则调用 process(ctx, T) 时桥方法会抛 ClassCastException。
     */
    @Override
    public IMHeartbeatInfo transForm(Object o) {
        return OBJECT_MAPPER.convertValue(o, IMHeartbeatInfo.class);
    }
}
