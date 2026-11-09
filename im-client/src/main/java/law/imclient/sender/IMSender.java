package law.imclient.sender;

import cn.hutool.core.collection.CollUtil;
import law.imclient.listener.EventListenerMulticaster;
import law.imclient.listener.MessageListenerMulticaster;
import law.imcommon.contant.IMRedisKey;
import law.imcommon.enums.IMCmdType;
import law.imcommon.enums.IMListenerType;
import law.imcommon.enums.IMSendCode;
import law.imcommon.enums.IMTerminalType;
import law.imcommon.model.*;
import law.imcommon.mq.RedisMQTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMSender {

    @Value("${spring.application.name}")
    private String appName;

    private final RedisMQTemplate redisMQTemplate;
    private final MessageListenerMulticaster listenerMulticaster;

    public <T> void sendPrivateMessage(IMPrivateMessage<T> message) {
        log.info("【im-client】【IMSender】【推送消息】message: {}", message.toString());
        IMBatchPrivateMessage<T> batch = new IMBatchPrivateMessage<>();
        batch.setSender(message.getSender());
        if (!Objects.isNull(message.getRecvId())) {
            batch.setRecvIds(List.of(message.getRecvId()));
        }
        batch.setRecvTerminals(message.getRecvTerminals());
        batch.setSendToSelf(message.getSendToSelf());
        batch.setSendResult(message.getSendResult());
        batch.setData(message.getData());
        sendBatchPrivateMessage(batch);
    }

    public <T> void sendBatchPrivateMessage(IMBatchPrivateMessage<T> message) {
        List<Long> recvIds = message.getRecvIds();
        List<Integer> recvTerminals = message.getRecvTerminals();
        List<IMUserInfo> offlineUsers = new ArrayList<>();
        IMUserInfo sender = message.getSender();
        // 接收方：redisKey → 接收用户信息（LinkedHashMap 保证遍历顺序与下方 multiGet 结果下标一致）
        Map<String, IMUserInfo> recvKeyToUser = new LinkedHashMap<>();
        if (!CollUtil.isEmpty(recvIds) && !CollUtil.isEmpty(recvTerminals)) {
            for (Integer terminal : recvTerminals) {
                for (Long id : recvIds) {
                    String key = IMRedisKey.userServerIdKey(id, terminal);
                    recvKeyToUser.put(key, new IMUserInfo(id, terminal));
                }
            }
        }

        // 发送端其他终端 与 selfKeys 按下标一一对应，用于根据 multiGet 结果推送同步消息
        List<String> selfKeys = new ArrayList<>();
        List<Integer> selfOtherTerminals = null;
        if (message.getSendToSelf()) {
            Long senderId = sender.getId();
            List<Integer> terminals = IMTerminalType.codes();
            selfOtherTerminals = new ArrayList<>(terminals.size());
            for (Integer terminal : terminals) {
                if (terminal.equals(sender.getTerminal())) {
                    continue;
                }
                selfKeys.add(IMRedisKey.userServerIdKey(senderId, terminal));
                selfOtherTerminals.add(terminal);
            }
        }

        int recvKeyCount = recvKeyToUser.size();
        int selfKeyCount = selfKeys.size();;
        int totalKeys = recvKeyCount + selfKeyCount;
        List<Object> serverIds = Collections.emptyList();
        if (totalKeys > 0) {
            List<String> keys = new ArrayList<>(totalKeys);
            keys.addAll(recvKeyToUser.keySet());
             keys.addAll(selfKeys);
            serverIds = redisMQTemplate.opsForValue().multiGet(keys);
        }
        // 按 IM-server 分组后写入私聊队列；无 serverId 的记入离线列表
        if (!recvKeyToUser.isEmpty()) {
            Map<Integer, List<IMUserInfo>> serverMap = new HashMap<>(16);
            int idx = 0;
            for (Map.Entry<String, IMUserInfo> entry : recvKeyToUser.entrySet()) {
                // 不要强转 Integer：serverId 在 Redis 里可能是 Long，
                // 一旦数值超 int 范围或序列化方式变化就会 ClassCastException
                Object raw = serverIds.get(idx++);
                Integer serverId = raw instanceof Number number ? number.intValue() : null;
                IMUserInfo recvUser = entry.getValue();
                if (serverId != null) {
                    serverMap.computeIfAbsent(serverId, k -> new ArrayList<>()).add(recvUser);
                } else {
                    // 记录离线设备
                    offlineUsers.add(recvUser);
                }
            }
            for (Map.Entry<Integer, List<IMUserInfo>> entry : serverMap.entrySet()) {
                // 推送在线设备
                pushPrivateMessage(entry.getKey(), sender, entry.getValue(), message.getSendResult(),
                        message.getData());
            }
        }

        // 自己的其它终端在线则入队；sendResult 固定 false，与单条私聊「同步给自己」行为一致
        if (!selfKeys.isEmpty()) {
            Long senderId = sender.getId();
            for (int i = 0; i < selfKeys.size(); i++) {
                Integer serverId = (Integer)serverIds.get(recvKeyCount + i);
                if (serverId != null) {
                    List<IMUserInfo> receivers = List.of(new IMUserInfo(senderId, selfOtherTerminals.get(i)));
                    pushPrivateMessage(serverId, sender, receivers, false, message.getData());
                }
            }
        }

        // 需要回推发送结果时，将离线接收方一次性回调给业务侧
        if (message.getSendResult() && !offlineUsers.isEmpty()) {
            IMBatchSendResult<T> result = new IMBatchSendResult<>();
            result.setSender(message.getSender());
            result.setReceivers(offlineUsers);
            result.setCode(IMSendCode.NOT_ONLINE.code());
            result.setData(message.getData());
            listenerMulticaster.multicast(IMListenerType.PRIVATE_MESSAGE, List.of(result));
        }
    }

    public Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        // 把所有用户的key都存起来
        Map<String, IMUserInfo> userMap = new LinkedHashMap<>();
        for (Long id : userIds) {
            for (Integer terminal : IMTerminalType.codes()) {
                String key = IMRedisKey.userServerIdKey(id, terminal);
                userMap.put(key, new IMUserInfo(id, terminal));
            }
        }
        // 批量拉取
        List<Object> serverIds = redisMQTemplate.opsForValue().multiGet(userMap.keySet());
        int idx = 0;
        Map<Long, List<IMTerminalType>> onlineMap = new HashMap<>();
        for (Map.Entry<String, IMUserInfo> entry : userMap.entrySet()) {
            // serverid有值表示用户在线
            if (serverIds.get(idx++) != null) {
                IMUserInfo userInfo = entry.getValue();
                List<IMTerminalType> terminals = onlineMap.computeIfAbsent(userInfo.getId(), o -> new LinkedList<>());
                terminals.add(IMTerminalType.fromCode(userInfo.getTerminal()));
            }
        }
        // 去重并返回
        return onlineMap;
    }

    /** 封装私聊 {@link IMRecvInfo} 并入对应 IM-server 的 Redis 私聊队列 */
    private <T> void pushPrivateMessage(Integer serverId, IMUserInfo sender, List<IMUserInfo> receivers,
                                        Boolean sendResult, T data) {
        IMRecvInfo recvInfo = new IMRecvInfo();
        recvInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
        recvInfo.setSender(sender);
        recvInfo.setReceivers(receivers);
        recvInfo.setServiceName(appName);
        recvInfo.setSendResult(sendResult);
        recvInfo.setData(data);
        String queueKey = String.join(":", IMRedisKey.IM_MESSAGE_PRIVATE_QUEUE, serverId.toString());
        redisMQTemplate.opsForList().rightPush(queueKey, recvInfo);
    }

    public Boolean isOnline(Long userId) {
        List<String> keys = new ArrayList<>(IMTerminalType.codes().size());
        for (Integer terminal : IMTerminalType.codes()) {
            keys.add(IMRedisKey.userServerIdKey(userId, terminal));
        }
        Long count = redisMQTemplate.countExistingKeys(keys);
        return count != null && count > 0;
    }

    public Boolean isOnline(Long userId, IMTerminalType terminal) {
        String key = IMRedisKey.userServerIdKey(userId, terminal.code());
        return redisMQTemplate.hasKey(key);
    }

    /**
     * 强制用户所有在线终端下线
     */
    public void forceLogout(Long userId, Integer type, String reason) {
        for (Integer terminal : IMTerminalType.codes()) {
            String key = IMRedisKey.userServerIdKey(userId, terminal);
            Object serverId = redisMQTemplate.opsForValue().get(key);
            if (Objects.isNull(serverId)) {
                continue;
            }
            IMForceLogoutInfo logoutInfo = new IMForceLogoutInfo();
            logoutInfo.setUserId(userId);
            logoutInfo.setTerminal(terminal);
            logoutInfo.setType(type);
            logoutInfo.setReason(reason);
            String queueKey = String.join(":", IMRedisKey.IM_USER_FORCE_LOGOUT_QUEUE, serverId.toString());
            redisMQTemplate.opsForList().rightPush(queueKey, logoutInfo);
        }
    }

    /**
     * 发送系统消息
     */
    public <T> void sendSystemMessage(IMSystemMessage<T> message) {
        // 根据群聊每个成员所连的IM-server，进行分组
        Map<String, IMUserInfo> sendMap = new LinkedHashMap<>();
        // 遍历所有终端设备
        for (Integer terminal : message.getRecvTerminals()) {
            message.getRecvIds().forEach(id -> {
                // 根据userId与设备获取server key
                String key = IMRedisKey.userServerIdKey(id, terminal);
                sendMap.put(key, new IMUserInfo(id, terminal));
            });
        }

        // 批量拉取
        List<Object> serverIds = redisMQTemplate.opsForValue().multiGet(sendMap.keySet());

        Map<Integer, List<IMUserInfo>> serverMap = new HashMap<>(); // 在线列表
        List<IMUserInfo> offLineUsers = new LinkedList<>(); // 离线列表
        int idx = 0;

        for (Map.Entry<String, IMUserInfo> entry : sendMap.entrySet()) {
            Integer serverId = (Integer)serverIds.get(idx++);
            if (!Objects.isNull(serverId)) {
                List<IMUserInfo> list = serverMap.computeIfAbsent(serverId, o -> new LinkedList<>());
                list.add(entry.getValue());
            } else {
                // 加入离线列表
                offLineUsers.add(entry.getValue());
            }
        }

        // 逐个server发送
        for (Map.Entry<Integer, List<IMUserInfo>> entry: serverMap.entrySet()) {
            IMRecvInfo imRecvInfo = new IMRecvInfo();
            imRecvInfo.setCmd(IMCmdType.SYSTEM_MESSAGE.code());
            imRecvInfo.setReceivers(new LinkedList<>(entry.getValue()));
            imRecvInfo.setServiceName(appName);
            imRecvInfo.setSendResult(message.getSendResult());
            imRecvInfo.setData(message.getData());
            // 推送至队列
            String key = String.join(":", IMRedisKey.IM_MESSAGE_SYSTEM_QUEUE, entry.getKey().toString());
            redisMQTemplate.opsForList().rightPush(key, imRecvInfo);
        }
        if (message.getSendResult() && !offLineUsers.isEmpty()) {
            // 对离线用户回复消息状态
            IMBatchSendResult<T> result = new IMBatchSendResult<>();
            result.setReceivers(offLineUsers);
            result.setCode(IMSendCode.NOT_ONLINE.code());
            result.setData(message.getData());
            listenerMulticaster.multicast(IMListenerType.SYSTEM_MESSAGE, List.of(result));
        }
    }
}
