package law.implatform.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import law.imcommon.util.R;
import law.implatform.annotation.RepeatSubmit;
import law.implatform.entity.FriendRequest;
import law.implatform.model.dto.FriendRequestBTO;
import law.implatform.model.vo.FriendVO;
import law.implatform.service.FriendRequestService;
import law.implatform.service.FriendService;
import law.implatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友
 */
@Slf4j
@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final FriendRequestService friendRequestService;

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public R<List<FriendVO>> findFriends(@RequestParam(defaultValue = "0") Long version) {
        return R.success(friendService.findFriends(version));
    }

    /**
     * 查找好友信息
     */
    @GetMapping("/find/{friendId}")
    public R<FriendVO> findFriend(@NotNull(message = "好友id不可为空") @PathVariable Long friendId) {
        return R.success(friendService.findFriend(friendId));
    }

    /**
     * 添加好友
     * 双方建立好友关系
     */
    @RepeatSubmit
    @PostMapping("/add")
    public R addFriend(@NotNull(message = "好友id不可为空") @RequestParam Long friendId) {
        friendService.addFriend(SecurityUtils.getUserId(),friendId);
        return R.success();
    }

    /**
     * 获取好友申请列表
     */
    @GetMapping("/request/list")
    public R<List<FriendRequest>> findRequestList() {
        List<FriendRequest> list = friendRequestService.findFriendRequestList();
        return R.success(list);
    }

    /**
     * 处理好友申请
     */
    @PostMapping("/request/handle")
    public R handleFriendRequest(@Valid @RequestBody FriendRequestBTO friendRequestBTO) {
        friendRequestService.handleFriendRequest(friendRequestBTO);
        return R.success();
    }
}
