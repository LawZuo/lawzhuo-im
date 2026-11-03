package law.imserver.netty.processor.factory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import law.imcommon.contant.IMConstant;
import law.imcommon.contant.IMRedisKey;
import law.imcommon.enums.IMCmdType;
import law.imcommon.enums.IMEventType;
import law.imcommon.enums.IMForceLogoutType;
import law.imcommon.jwt.JwtService;
import law.imcommon.model.*;
import law.imcommon.mq.RedisMQTemplate;
import law.imserver.constant.ChannelAttrKey;
import law.imserver.netty.IMServerGroup;
import law.imserver.netty.UserChannelCtxMap;
import law.imserver.netty.processor.AbstractMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 登录处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginMessageProcessor extends AbstractMessageProcessor<IMLoginInfo> {

    private final RedisMQTemplate redisMQTemplate;
    private final JwtService jwtService;


    @Override
    public void process(ChannelHandlerContext ctx, IMLoginInfo loginInfo) {
        log.info("【im-server】收到登录通知，设备: {}", loginInfo.getDevId());

        // 校验 token：签名、过期时间、issuer、token 类型（必须是 access token）
        UserSession session;
        try {
            session = jwtService.parseAccessToken(loginInfo.getAccessToken());
        } catch (JwtException e) {
            log.warn("【im-server】token 校验不通过，关闭连接: {}", e.getMessage());
            ctx.channel().close();
            return;
        }

        Long userId = session.getUserId();
        Integer terminal = session.getTerminal();
        // TODO 封禁/注销等账号拒绝建立长连接
        log.info("【im-server】用户登录，userId:{}, terminal:{}", userId, terminal);

        // 检查是否有其他设备在线
        String key = IMRedisKey.userServerIdKey(userId, terminal);
        Object serverId = redisMQTemplate.opsForValue().get(key);
        log.info("【im-server】用户登录，redis key: {}, serverId:{}", key, serverId);

        if (!Objects.isNull(serverId)) {
            // 用户已在线，强制使其下线
            log.info("【im-server】用户登录，已存在的服务id: {}, redis存储的服务id: {}", IMServerGroup.serverId, serverId);

            // 判断serverId,服务器一致
            if (IMServerGroup.serverId.equals(Long.parseLong(serverId.toString()))) {
                log.info("两次连的同一个服务器");
                ChannelHandlerContext context = UserChannelCtxMap.getChannelCtx(userId, terminal);

                // 同一节点、同一连接（重复发登录帧）channel id 相同 → 什么都不做，继续往下走
                if (context != null && !ctx.channel().id().equals(context.channel().id())) {
                    AttributeKey<String> devIdAttr = AttributeKey.valueOf(ChannelAttrKey.DEVICE_ID);
                    String devId = context.channel().attr(devIdAttr).get();
                    log.info("【im-server】用户登录，context devId: {}", devId);
                    log.info("【im-server】用户登录，loginInfo devId: {}", loginInfo.getDevId());

                    // 同一节点、不同连接（异地登录，比如换了个浏览器）
                    if (StrUtil.isEmpty(loginInfo.getDevId()) || !loginInfo.getDevId().equals(devId)) {
                        // 不允许多地登录,强制下线
                        IMForceLogoutData data = new IMForceLogoutData();
                        data.setType(IMForceLogoutType.KICKED.code());
                        IMSendInfo<IMForceLogoutData> sendInfo = new IMSendInfo<>();
                        sendInfo.setCmd(IMCmdType.FORCE_LOGOUT.code());
                        sendInfo.setData(data);
                        context.channel().writeAndFlush(sendInfo).addListener(ChannelFutureListener.CLOSE);
                        log.info("【im-server】用户登录，异地登录，强制下线,userId:{},终端:{}", userId, terminal);
                    }
                }

            } else {
                // 连的是不同的服务器，投递下线命令到上次连的服务器
                log.info("【im-server】用户登录，两次连的不是同一个服务器");
                IMForceLogoutInfo logoutInfo = new IMForceLogoutInfo();
                logoutInfo.setUserId(userId);
                logoutInfo.setTerminal(terminal);
                logoutInfo.setDevId(loginInfo.getDevId());
                logoutInfo.setType(IMForceLogoutType.KICKED.code());
                String queueKey = StrUtil.join(":", IMRedisKey.IM_USER_FORCE_LOGOUT_QUEUE, serverId);
                redisMQTemplate.opsForList().rightPush(queueKey, logoutInfo);
            }
        }
        // 绑定用户和channel
        UserChannelCtxMap.addChannelCtx(userId, terminal, ctx);

        // 设置用户id属性
        AttributeKey<Object> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
        ctx.channel().attr(userIdAttr).set((userId));
        // 设置用户终端类型
        AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
        ctx.channel().attr(terminalAttr).set(terminal);
        // 设置用户设备id
        AttributeKey<String> devIdAttr = AttributeKey.valueOf(ChannelAttrKey.DEVICE_ID);
        ctx.channel().attr(devIdAttr).set(loginInfo.getDevId());
        // 初始化心跳次数
        AttributeKey<Long> heartBeatAttr = AttributeKey.valueOf(ChannelAttrKey.HEARTBEAT_TIMES);
        ctx.channel().attr(heartBeatAttr).set(0L);
        // 在redis上记录每个user的channelId，超时无心跳则自动过期
        redisMQTemplate.opsForValue().set(key, IMServerGroup.serverId, IMConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
        // 推送用户上线事件给业务层
        IMUserEvent event = new IMUserEvent();
        event.setEventType(IMEventType.ONLINE.code());
        event.setUserInfo(new IMUserInfo(userId, terminal));
        key = IMRedisKey.IM_USER_EVENT_QUEUE;
        redisMQTemplate.opsForList().rightPush(key, event);
        // 响应ws
        IMSendInfo<Object> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(IMCmdType.LOGIN.code());
        log.info("【im-server】用户登录，响应数据, sendInfo: {}", sendInfo.toString());
        ctx.channel().writeAndFlush(sendInfo)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    /**
     * 解码器把 data 解析成了 LinkedHashMap，这里转成具体类型，
     * 否则调用 process(ctx, T) 时桥方法会抛 ClassCastException。
     */
    @Override
    public IMLoginInfo transForm(Object o) {
        HashMap map = (HashMap) o;
        return BeanUtil.fillBeanWithMap(map, new IMLoginInfo(), false);
    }
}
