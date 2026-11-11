package law.implatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import law.imclient.IMClient;
import law.imcommon.model.IMPrivateMessage;
import law.imcommon.model.IMUserInfo;
import law.imcommon.model.UserSession;
import law.imcommon.model.vo.PrivateMessageVO;
import law.implatform.entity.FriendRequest;
import law.implatform.entity.PrivateMessage;
import law.implatform.enums.FriendRequestStatus;
import law.implatform.enums.MessageStatus;
import law.implatform.enums.MessageType;
import law.implatform.exception.GlobalException;
import law.implatform.mapper.FriendRequestMapper;
import law.implatform.model.dto.FriendRequestAddBTO;
import law.implatform.model.dto.FriendRequestHandleBTO;
import law.implatform.service.FriendRequestService;
import law.implatform.service.FriendService;
import law.implatform.service.PrivateMessageService;
import law.implatform.util.ConvUtil;
import law.implatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl extends ServiceImpl<FriendRequestMapper, FriendRequest> implements FriendRequestService {

    private final FriendService friendService;
    private final PrivateMessageService privateMessageService;
    private final IMClient imClient;

    @Override
    public List<FriendRequest> findFriendRequestList() {
        UserSession session = SecurityUtils.getSession();
        Long userId = session.getUserId(); // 用户id
        LambdaQueryWrapper<FriendRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendRequest::getFromUserId, userId);
        return this.list(queryWrapper);
    }

    @Override
    public void handleFriendRequest(FriendRequestHandleBTO dto) {
        UserSession session = SecurityUtils.getSession();
        Long toUserId = session.getUserId(); // 用户id

        if (dto.getStatus() == 1) {
            // 通过
            log.info("用户同意好友申请，formUserId:{}, toUserId:{}", dto.getFromUserId(), toUserId);
            friendService.addFriend(dto.getFromUserId(), toUserId);
        } else if (dto.getStatus() == 2) {
            // 拒绝 不进行任何操作
            log.info("用户取消好友申请，formUserId:{}, toUserId:{}", dto.getFromUserId(), toUserId);
        }

        // 删除请求
        LambdaQueryWrapper<FriendRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendRequest::getFromUserId, dto.getFromUserId());
        queryWrapper.eq(FriendRequest::getToUserId, toUserId);
        boolean flag = this.remove(queryWrapper);
        log.info("删除好友请求是否成功:{}", flag);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFriendRequest(FriendRequestAddBTO dto) {
        UserSession session = SecurityUtils.getSession();
        Long fromUserId = session.getUserId(); // 发起用户id
        Long toUserId = dto.getToUserId(); // 接收用户id
        log.info("【im-platform】【FriendService】【添加好友-01】 toUserId:{}, fromUserId: {}", toUserId, fromUserId);

        // 添加好友
        if (toUserId.equals(fromUserId)) {
            throw new GlobalException("不允许添加自己为好友");
        }

        // 查看重复申请,存在申请不做处理
        LambdaQueryWrapper<FriendRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendRequest::getFromUserId, fromUserId);
        queryWrapper.eq(FriendRequest::getToUserId, toUserId);
        FriendRequest friendRequest = this.getOne(queryWrapper);
        if (friendRequest != null) return;

        // 判断是否已经是好友
        Boolean isFriend = friendService.isFriend(fromUserId, toUserId);
        if (isFriend) {
            throw new GlobalException("好友已存在");
        }

        // 发起好友请求
        FriendRequest vo = new FriendRequest();
        vo.setFromUserId(fromUserId);
        vo.setToUserId(toUserId);
        vo.setMessage(dto.getMessage());
        vo.setStatus(FriendRequestStatus.WAIT.code());
        vo.setApplyTime(new Date());
        this.save(vo);

        // 发送通知
        sendRequestMessage(session, toUserId);

    }

    // 发起消息通知
    void sendRequestMessage(UserSession session, Long toUserId) {

        PrivateMessage message = new PrivateMessage();
        message.setSendId(session.getUserId());
        message.setRecvId(toUserId);
        message.setConvKey(ConvUtil.buildConvKey(message.getSendId(), message.getRecvId()));
        message.setContent("收到好友请求");
        message.setSendTime(new Date());
        message.setStatus(MessageStatus.PENDING.code());
        message.setType(MessageType.FRIEND_APPLY.code());
        privateMessageService.saveMessage(message);
        // 推送给对方
        PrivateMessageVO messageInfo = new PrivateMessageVO();
        BeanUtils.copyProperties(message, messageInfo);
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(toUserId);
        sendMessage.setSendToSelf(false);
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);
    }
}
