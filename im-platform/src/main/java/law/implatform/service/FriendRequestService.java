package law.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import law.implatform.entity.FriendRequest;
import law.implatform.model.dto.FriendRequestAddBTO;
import law.implatform.model.dto.FriendRequestHandleBTO;

import java.util.List;

public interface FriendRequestService extends IService<FriendRequest> {

    /**
     * 获取好友请求列表
     */
    List<FriendRequest> findFriendRequestList();

    /**
     * 处理好友请求
     */
    void handleFriendRequest(FriendRequestHandleBTO dto);

    /**
     * 发起好友请求
     */
    void addFriendRequest(FriendRequestAddBTO dto);
}
