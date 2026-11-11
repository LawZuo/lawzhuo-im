package law.implatform.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum FriendRequestStatus {

    WAIT(0, "待处理"),
    ACCEPT(1, "通过"),
    REJECT(2, "拒绝"),
    WITHDRAW(3, "撤回");

    private final Integer code;
    private final String desc;

    public Integer code() {
        return this.code;
    }

    public String desc() {
        return this.desc;
    }
}
