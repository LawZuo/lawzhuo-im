package law.implatform.enums;

import lombok.AllArgsConstructor;

/**
 * 0-9: 真正的消息，需要存储到数据库
 * 10-19: 状态类消息: 撤回、已读、回执
 * 20-29: 提示类消息: 在会话中间显示的提示
 * 30-39: UI交互类消息: 显示加载状态等
 * 40-49: 操作交互类消息: 语音通话、视频通话消息等
 * 50-60: 后台操作类消息: 用户封禁、群组封禁等
 * 80-89: 好友变化消息
 * 90-99: 群聊变化消息
 * 100-199: 单人语音通话rtc信令
 * 200-299: 多人语音通话rtc信令
 *
 */

@AllArgsConstructor
public enum MessageType {

    TEXT(0, "文字消息"),
    TIP_TEXT(21,"文字提示"),
    RECALL(10, "撤回"),

    FRIEND_NEW(80, "新增好友"),
    FRIEND_DEL(81, "删除好友"),
    FRIEND_ONLINE(82, "好友在线状态变化"),
    FRIEND_DND(83, "好友免打扰"),
    FRIEND_TOP(84, "好友会话置顶");

    private final Integer code;

    private final String desc;


    public Integer code() {
        return this.code;
    }
}
