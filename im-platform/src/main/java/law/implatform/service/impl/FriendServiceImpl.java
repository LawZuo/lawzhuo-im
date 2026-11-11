package law.implatform.service.impl;

import ch.qos.logback.classic.spi.EventArgUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import law.imclient.IMClient;
import law.imcommon.enums.IMTerminalType;
import law.imcommon.model.IMBatchPrivateMessage;
import law.imcommon.model.IMPrivateMessage;
import law.imcommon.model.IMUserInfo;
import law.imcommon.model.UserSession;
import law.imcommon.mq.RedisMQTemplate;
import law.imcommon.util.R;
import law.implatform.annotation.RedisLock;
import law.implatform.contant.RedisKey;
import law.implatform.entity.Friend;
import law.implatform.entity.PrivateMessage;
import law.implatform.entity.User;
import law.implatform.enums.MessageStatus;
import law.implatform.enums.MessageType;
import law.implatform.exception.GlobalException;
import law.implatform.mapper.FriendMapper;
import law.implatform.mapper.UserMapper;
import law.implatform.model.dto.FriendAddDTO;
import law.implatform.model.dto.FriendTopDTO;
import law.implatform.model.dto.FriendUpdateDTO;
import law.implatform.model.vo.FriendVO;
import law.imcommon.model.vo.PrivateMessageVO;
import law.implatform.model.vo.UserOnlineVO;
import law.implatform.util.SecurityUtils;
import law.implatform.service.FriendService;
import law.implatform.service.PrivateMessageService;
import law.implatform.util.ConvUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final IMClient imClient;

    @Lazy
    @Autowired
    private PrivateMessageService privateMessageService;


    @Override
    public void sendOnlineStatus(Long userId, Integer terminal) {
        // TODO 推送在线状态给所有好友
        log.info("【im-platform】Friend, 收到用户状态: userId: {}, 终端: {}", userId, terminal);
    }

    @Override
    @Cacheable(cacheNames = "im:cache:friend", key =  "#userId1+':'+#userId2")
    public Boolean isFriend(Long userId1, Long userId2) {
        // 是否是好友
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper();
        wrapper.eq(Friend::getUserId, userId1);
        wrapper.eq(Friend::getFriendId, userId2);
        wrapper.eq(Friend::getDeleted, false);
        return friendMapper.exists(wrapper);
    }

    @Override
    public List<FriendVO> findFriends(Long version) {
        // 查询当前用户的所有好友
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId);
        queryWrapper.gt(version > 0, Friend::getVersion, version);
        List<Friend> friends = friendMapper.selectList(queryWrapper);
        return friends.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public FriendVO findFriend(Long friendId) {
        // 查询指定的某个好友信息
        UserSession session = SecurityUtils.getSession();
        LambdaQueryWrapper<Friend> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Friend::getUserId, session.getUserId());
        wrapper.eq(Friend::getFriendId, friendId);
        Friend friend = this.getOne(wrapper);
        if (Objects.isNull(friend)) {
            throw new GlobalException("对方不是您的好友");
        }
        return convert(friend);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @RedisLock(prefixKey = RedisKey.IM_LOCK_FRIEND_ADD,
            key = "T(law.implatform.util.ConvUtil).buildConvKey(#userId, #friendId)")
    public void addFriend(Long userId, Long friendId) {
        log.info("【im-platform】【FriendService】【添加好友-01】 userId:{}, friendId: {}", userId, friendId);

        // 添加好友
        if (friendId.equals(userId)) {
            throw new GlobalException("不允许添加自己为好友");
        }

        // 发起好友请求


        // 互相绑定好友关系
        FriendServiceImpl proxy = (FriendServiceImpl) AopContext.currentProxy();
        proxy.bindFriend(userId, friendId);
        proxy.bindFriend(friendId, userId);
        // 推送添加好友提示
        sendAddTipMessage(userId, friendId);
    }

    /**
     * ===================================
     */


    @Override
    public List<Long> findFriendIds() {
        // 查询好友id列表
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId);
        queryWrapper.eq(Friend::getDeleted, false);
        queryWrapper.select(Friend::getFriendId);
        List<Friend> friends = this.list(queryWrapper);
        return friends.stream().map(Friend::getFriendId).collect(Collectors.toList());
    }

    @Override
    public List<Friend> findByFriendIds(List<Long> friendIds) {
        // 查询用户的所有好友
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId);
        queryWrapper.in(Friend::getFriendId, friendIds);
        queryWrapper.eq(Friend::getDeleted, false);
        return friendMapper.selectList(queryWrapper);
    }

    @Override
    @CacheEvict(cacheNames = "im:cache:friend", key = "#userId+':'+#friendId")
    public void bindFriend(Long userId, Long friendId) {
        // 单向绑定好友关系
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId);
        Friend friend = friendMapper.selectOne(queryWrapper);
        if (Objects.isNull(friend)) {
            friend = new Friend();
        }
        friend.setVersion(getNextVersion());
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        User friendInfo = userMapper.selectById(friendId);
        friend.setFriendHeadImage(friendInfo.getHeadImageThumb());
        friend.setFriendNickName(friendInfo.getNickName());
        friend.setDeleted(false);
        this.saveOrUpdate(friend);
        // 推送同步消息
//        sendSyncDndMessage(dto.getFriendId(), dto.getIsDnd());
    }

//    @Override
//    public List<UserOnlineVO> findOnlineTerminals() {
//        // 查询好友id列表
//        List<Long> ids = findFriendIds();
//        // 查询在线的好友终端
//        Map<Long, List<IMTerminalType>> onlineMap = imClient.getOnlineTerminal(ids);
//        // 返回vo
//        List<UserOnlineVO> vos = new ArrayList<>();
//        onlineMap.forEach((userId, terminals) -> terminals.forEach(terminal -> {
//            UserOnlineVO vo = new UserOnlineVO();
//            vo.setUserId(userId);
//            vo.setTerminal(terminal.code());
//            vo.setOnline(true);
//            vos.add(vo);
//        }));
//        return vos;
//    }

    /**
     * 单向解除好友关系
     *
     * @param userId   用户id
     * @param friendId 好友的用户id
     */
//    @CacheEvict(key = "#userId+':'+#friendId")
//    public void unbindFriend(Long userId, Long friendId) {
//        // 逻辑删除
//        LambdaUpdateWrapper<Friend> wrapper = new LambdaUpdateWrapper<>();
//        wrapper.eq(Friend::getUserId, userId);
//        wrapper.eq(Friend::getFriendId, friendId);
//        wrapper.set(Friend::getDeleted, true);
//        wrapper.set(Friend::getVersion, getNextVersion());
//        this.update(wrapper);
//        // 推送好友变化信息
//        sendDelFriendMessage(userId, friendId);
//    }

    /**
     * 查当前用户名下、未删除的好友记录
     */
//    private Friend getOwnedFriend(Long userId, Long friendId) {
//        return friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
//                .eq(Friend::getUserId, userId)
//                .eq(Friend::getFriendId, friendId)
//                .eq(Friend::getDeleted, false));
//    }

//    List<Long> loadAllFriendIds(Long userId) {
//        LambdaQueryWrapper<Friend> wrapper = Wrappers.lambdaQuery();
//        wrapper.eq(Friend::getUserId, userId);
//        wrapper.eq(Friend::getDeleted, false);
//        wrapper.select(Friend::getFriendId);
//        List<Friend> friends = this.list(wrapper);
//        return friends.stream().map(Friend::getFriendId).collect(Collectors.toList());
//    }

    @Override
    public Long getNextVersion() {
        // 获取版本信息
        String key = StrUtil.join(":", RedisKey.IM_FRIEND_MAX_VERSION);
        if (redisTemplate.hasKey(key)) {
            return redisTemplate.opsForValue().increment(key);
        } else {
            LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(Friend::getVersion);
            queryWrapper.last("limit 1");
            Friend friend = friendMapper.selectOne(queryWrapper);
            Long version = Objects.isNull(friend) ? 1 : friend.getVersion() + 1;
            redisTemplate.opsForValue().set(key, version);
            return version;
        }
    }

    void sendAddTipMessage(Long userId, Long friendId) {
        log.info("【im-platform】【FriendService】【添加好友-02】添加好友成功，开始缓存打招呼消息");
        // 推送添加好友消息
        UserSession session = SecurityUtils.getSession();
        log.info("从缓存中获取用户信息, session: {}, {}", session.getUserId(), session.getUserName());
        PrivateMessage message = new PrivateMessage();
        message.setSendId(session.getUserId());
        message.setRecvId(friendId);
        message.setConvKey(ConvUtil.buildConvKey(message.getSendId(), message.getRecvId()));
        message.setContent("你们已成为好友，现在可以开始聊天了");
        message.setSendTime(new Date());
        message.setStatus(MessageStatus.PENDING.code());
        message.setType(MessageType.TIP_TEXT.code());
        privateMessageService.saveMessage(message);
        log.info("【im-platform】【FriendService】【添加好友-03】缓存消息成功，开始推送添加好友消息");
        // 推给对方
        PrivateMessageVO messageInfo = new PrivateMessageVO();
        BeanUtils.copyProperties(message, messageInfo);
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(friendId);
        sendMessage.setSendToSelf(false);
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);
        // 推给自己
        sendMessage.setRecvId(session.getUserId());
        imClient.sendPrivateMessage(sendMessage);
    }

    private FriendVO convert(Friend f) {
        FriendVO vo = new FriendVO();
        vo.setId(f.getFriendId());
        vo.setHeadImage(f.getFriendHeadImage());
        vo.setNickName(f.getFriendNickName());
        vo.setDeleted(f.getDeleted());
        vo.setIsDnd(f.getIsDnd());
        vo.setIsTop(f.getIsTop());
        vo.setVersion(f.getVersion());
        return vo;
    }
}
