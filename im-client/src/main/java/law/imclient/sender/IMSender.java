package law.imclient.sender;

import cn.hutool.core.collection.CollUtil;
import law.imcommon.contant.IMRedisKey;
import law.imcommon.enums.IMCmdType;
import law.imcommon.enums.IMTerminalType;
import law.imcommon.model.IMBatchPrivateMessage;
import law.imcommon.model.IMPrivateMessage;
import law.imcommon.model.IMRecvInfo;
import law.imcommon.model.IMUserInfo;
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
                    log.info("recvKeyToUser集合存储数据, key: {}, recvId: {}, terminal: {}", key, id, terminal);
                }
            }
        }

        int recvKeyCount = recvKeyToUser.size();
        int selfKeyCount = 0;
        int totalKeys = recvKeyCount + selfKeyCount;
        List<Object> serverIds = Collections.emptyList();
        if (totalKeys > 0) {
            List<String> keys = new ArrayList<>(totalKeys);
            keys.addAll(recvKeyToUser.keySet());
            // keys.addAll(selfKeys);
            serverIds = redisMQTemplate.opsForValue().multiGet(keys);
            log.info("从缓存获取数据, keys: {}, values: {}", keys, serverIds);
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
                    log.info("用户已离线, userId:{}, terminal:{}, key:{}",
                            recvUser.getId(), recvUser.getTerminal(), entry.getKey());
                    offlineUsers.add(recvUser);
                }
            }
            for (Map.Entry<Integer, List<IMUserInfo>> entry : serverMap.entrySet()) {
                log.info("用户在线:{}", entry.getValue());
                pushPrivateMessage(entry.getKey(), sender, entry.getValue(), message.getSendResult(),
                        message.getData());
            }
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
}
