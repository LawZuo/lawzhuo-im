package law.implatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import law.implatform.entity.FriendRequest;
import law.implatform.mapper.FriendRequestMapper;
import law.implatform.model.dto.FriendRequestBTO;
import law.implatform.service.FriendRequestService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendRequestServiceImpl extends ServiceImpl<FriendRequestMapper, FriendRequest> implements FriendRequestService {

    @Override
    public List<FriendRequest> findFriendRequestList() {
        return List.of();
    }

    @Override
    public void handleFriendRequest(FriendRequestBTO dto) {

    }
}
