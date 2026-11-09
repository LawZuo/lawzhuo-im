package law.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import law.imclient.IMClient;
import law.imcommon.model.IMPrivateMessage;
import law.imcommon.model.IMUserInfo;
import law.implatform.annotation.RedisLock;
import law.implatform.contant.Constant;
import law.implatform.contant.RedisKey;
import law.implatform.entity.PrivateMessage;
import law.implatform.enums.MessageStatus;
import law.implatform.enums.MessageType;
import law.implatform.exception.GlobalException;
import law.implatform.mapper.PrivateMessageMapper;
import law.implatform.model.dto.PrivateMessageDTO;
import law.imcommon.model.vo.PrivateMessageVO;
import law.implatform.util.SecurityUtils;
import law.implatform.service.FriendService;
import law.implatform.service.PrivateMessageService;
import law.imcommon.model.UserSession;
import law.implatform.util.ConvUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl extends ServiceImpl<PrivateMessageMapper, PrivateMessage> implements PrivateMessageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FriendService friendService;
    private final RedissonClient redissonClient;
    private final IMClient imClient;

    @Override
    public PrivateMessageVO sendMessage(PrivateMessageDTO dto) {
        // 1. 验证数据
        validMessage(dto);
        // 2. 检查好友信息
        UserSession session = SecurityUtils.getSession();
        Boolean isFriends = friendService.isFriend(session.getUserId(), dto.getRecvId());
        if (Boolean.FALSE.equals(isFriends)) {
            throw new GlobalException("您已不是对方好友，无法发送消息");
        }

        // 3. 保存消息
        PrivateMessage message = new PrivateMessage();
        BeanUtils.copyProperties(dto, message);
        message.setConvKey(ConvUtil.buildConvKey(session.getUserId(), dto.getRecvId()));
        message.setSendId(session.getUserId());
        message.setStatus(MessageStatus.PENDING.code());
        message.setSendTime(new Date());
        // 走代理触发分布式锁
        PrivateMessageServiceImpl proxy = (PrivateMessageServiceImpl) AopContext.currentProxy();
        proxy.saveMessage(message);

        // 推送消息
        PrivateMessageVO vo = new PrivateMessageVO();
        BeanUtils.copyProperties(message, vo);
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(vo.getRecvId());
        sendMessage.setData(vo);
        imClient.sendPrivateMessage(sendMessage);
        log.info("撤回私聊消息，发送id:{},接收id:{}，内容:{}", message.getSendId(), message.getRecvId(),
                message.getContent());
        return vo;
    }

    @Override
    @RedisLock(prefixKey = RedisKey.IM_LOCK_PRIVATE_MESSAGE_SAVE, key = "#message.convKey")
    public void saveMessage(PrivateMessage message) {
        log.info("【im-platform: PrivateMessageService】保存私聊消息, {}", message.toString());
        if (StrUtil.isEmpty(message.getLocalId())) {
            message.setLocalId(IdWorker.getIdStr());
        }
        message.setSeqNo(getNextSeqNo(message.getConvKey()));
        save(message);
        // 记录消息最大id
        String key = StrUtil.join(":", RedisKey.IM_PRIVATE_MESSAGE_MAX_ID, message.getConvKey());

        log.info("【im-platform: PrivateMessageService】缓存会话, key:{}, messageId:{}", key, message.getId());
        redisTemplate.opsForValue().set(key, message.getId(), Constant.MAX_OFFLINE_MESSAGE_DAYS, TimeUnit.DAYS);
    }

    @Override
    public List<PrivateMessageVO> loadOfflineMessage(Long minId) {
        long time = System.currentTimeMillis();
        UserSession session = SecurityUtils.getSession();
        // 获取当前用户的消息
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();

        Date minDate = DateUtils.addDays(new Date(), Math.toIntExact(-Constant.MAX_OFFLINE_MESSAGE_DAYS));
        wrapper.gt(PrivateMessage::getId, minId);
        wrapper.ge(PrivateMessage::getSendTime, minDate);
        wrapper.and(wp -> wp.eq(PrivateMessage::getSendId, session.getUserId()).or()
                .eq(PrivateMessage::getRecvId, session.getUserId()));
        wrapper.orderByDesc(PrivateMessage::getId);
        wrapper.last("limit " + Constant.MAX_OFFLINE_MESSAGE_SIZE);
        List<PrivateMessage> messages = this.list(wrapper);
        // 保证每个会话至少会拉到一条消息
        if (messages.size() >= Constant.MAX_OFFLINE_MESSAGE_SIZE) {
            messages = appendLastMessageInConversation(messages, minId);
        }
        // TODO 过滤删除的消息
        // 更新消息为送达状态
        List<Long> messageIds = messages.stream().filter(m -> m.getRecvId().equals(session.getUserId()))
                .filter(m -> m.getStatus().equals(MessageStatus.PENDING.code())).map(PrivateMessage::getId)
                .collect(Collectors.toList());
        if (!messageIds.isEmpty()) {
            LambdaUpdateWrapper<PrivateMessage> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.in(PrivateMessage::getId, messageIds);
            updateWrapper.set(PrivateMessage::getStatus, MessageStatus.DELIVERED.code());
            update(updateWrapper);
        }
        // 转换vo
        List<PrivateMessageVO> vos = messages.stream().map(m -> {
            PrivateMessageVO vo = new PrivateMessageVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).toList();
        log.info("拉取离线私聊消息,用户id:{},数量:{},耗时:{},minId:{}", session.getUserId(), vos.size(),
                System.currentTimeMillis() - time, minId);
        return vos.stream().sorted(Comparator.comparing(PrivateMessageVO::getId)).collect(Collectors.toList());
    }

    // 获取下一个版本号
    private Long getNextSeqNo(String convKey) {
        String key = StrUtil.join(":", RedisKey.IM_PRIVATE_MESSAGE_MAX_SEQ, convKey);
        Long seqNo = redisTemplate.opsForValue().increment(key);
        if (seqNo > 1L) {
            return seqNo;
        }
        redisTemplate.delete(key);
        RLock lock = redissonClient.getLock(RedisKey.IM_LOCK_PRIVATE_MESSAGE_MAX_SEQ);
        lock.lock();
        try {
            //Double check
            if (redisTemplate.hasKey(key)) {
                return redisTemplate.opsForValue().increment(key);
            }
            LambdaQueryWrapper<PrivateMessage> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(PrivateMessage::getConvKey, convKey);
            wrapper.orderByDesc(PrivateMessage::getSeqNo);
            wrapper.last("limit 1");
            PrivateMessage lastMessage = this.getOne(wrapper);
            seqNo = 1L;
            if (!Objects.isNull(lastMessage) && !Objects.isNull(lastMessage.getSeqNo())) {
                seqNo += lastMessage.getSeqNo();
            }
            redisTemplate.opsForValue().set(key, seqNo);
            return seqNo;
        } finally {
            lock.unlock();
        }
    }

    // 发送消息鉴权
    private void validMessage(PrivateMessageDTO message) {
        // 检查消息文本长度 消息类型 + 文本长度
        if (
                MessageType.TEXT.code().equals(message.getType()) &&
                        message.getContent().length() > Constant.MAX_MESSAGE_LENGTH
        ) {
            throw new GlobalException(String.format("消息长度不能大于%s个字符", Constant.MAX_MESSAGE_LENGTH));
        }

        // 检查文本类型
        try {
            if (!MessageType.TEXT.code().equals(message.getType())) {
                JSONUtil.parse(message.getContent());
            }
        } catch (Exception e) {
            throw new GlobalException("消息格式异常");
        }
    }

    List<PrivateMessage> appendLastMessageInConversation(List<PrivateMessage> messages, Long minId) {
        UserSession session = SecurityUtils.getSession();
        List<Long> fIds = friendService.findFriendIds();
        Set<Long> existIds = messages.stream().map(m -> getFriendId(session, m)).collect(Collectors.toSet());
        // 移除已经拉到消息的会话
        fIds.removeAll(existIds);
        if (fIds.isEmpty()) {
            return messages;
        }
        List<String> keys =
                fIds.stream().map(id -> buildMaxMessageIdKey(session.getUserId(), id)).collect(Collectors.toList());
        List<Object> maxMessageIds = redisTemplate.opsForValue().multiGet(keys);
        maxMessageIds =
                maxMessageIds.stream().filter(id -> !Objects.isNull(id) && Long.parseLong(id.toString()) > minId)
                        .collect(Collectors.toList());
        if (maxMessageIds.isEmpty()) {
            return messages;
        }
        LambdaQueryWrapper<PrivateMessage> wrapper = Wrappers.lambdaQuery();
        Date minDate = DateUtils.addDays(new Date(), Math.toIntExact(-Constant.MAX_OFFLINE_MESSAGE_DAYS));
        wrapper.ge(PrivateMessage::getSendTime, minDate);
        wrapper.in(PrivateMessage::getId, maxMessageIds);
        List<PrivateMessage> lastMessages = this.list(wrapper);
        messages.addAll(lastMessages);
        return messages;
    }

    private Long getFriendId(UserSession session, PrivateMessage message) {
        return session.getUserId().equals(message.getSendId()) ? message.getRecvId() : message.getSendId();
    }

    private String buildMaxMessageIdKey(Long userId1, Long userId2) {
        return StrUtil.join(":", RedisKey.IM_PRIVATE_MESSAGE_MAX_ID, ConvUtil.buildConvKey(userId1, userId2));
    }
}
