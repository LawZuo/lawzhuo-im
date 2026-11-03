package law.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import law.implatform.model.vo.PrivateMessageVO;
import law.implatform.util.SecurityUtils;
import law.implatform.service.FriendService;
import law.implatform.service.PrivateMessageService;
import law.imcommon.model.UserSession;
import law.implatform.util.ConvUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
}
