package law.implatform.listener;

import law.imclient.annotation.IMListener;
import law.imclient.listener.EventListener;
import law.imcommon.enums.IMEventType;
import law.imcommon.enums.IMListenerType;
import law.imcommon.model.IMUserEvent;
import law.imcommon.model.IMUserInfo;
import law.implatform.service.FriendService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 用户事件监听器
 */
@Slf4j
@AllArgsConstructor
@IMListener(type = IMListenerType.USER_EVENT)
public class UserEventListener implements EventListener {

    private final FriendService friendService;

    @Override
    public void process(List<IMUserEvent> events) {
        // TODO 向好友推送在线状态
        for (IMUserEvent event : events) {
            if (event.getEventType().equals(IMEventType.ONLINE.code())) {
                // 向好友推送在线状态
                IMUserInfo userInfo = event.getUserInfo();
                  friendService.sendOnlineStatus(userInfo.getId(), userInfo.getTerminal());
                log.info("【im-platform】用户上线,id:{},终端：{}", userInfo.getId(), userInfo.getTerminal());
            } else if (event.getEventType().equals(IMEventType.OFFLINE.code())) {
                // 向好友推送在线状态
                IMUserInfo userInfo = event.getUserInfo();
                  friendService.sendOnlineStatus(userInfo.getId(), userInfo.getTerminal());
                log.info("【im-platform】用户下线,id:{},终端：{}", userInfo.getId(), userInfo.getTerminal());
            }
        }
    }
}
