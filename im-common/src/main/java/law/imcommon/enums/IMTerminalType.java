package law.imcommon.enums;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum IMTerminalType {
    /**
     * web
     */
    WEB(0, "web"),
    /**
     * app
     */
    APP(1, "app"),
    /**
     * pc
     */
    PC(2, "pc"),
    /**
     * 未知
     */
    UNKNOW(-1, "未知");

    private final Integer code;

    private final String desc;


    public static IMTerminalType fromCode(Integer code) {
        for (IMTerminalType typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }

    /**
     * 所有"真实终端"的类型码（不含 UNKNOW）。
     *
     * <p>UNKNOW(-1) 只是兜底标识，永远不会建立长连接，也不该出现在
     * 消息路由的终端列表里；否则每次发送都会去查一个不存在的
     * {@code im:user:server_id:{userId}:-1}，产生大量"用户已离线"噪音。
     */
    public static List<Integer> codes() {
        return Arrays.stream(values())
                .filter(type -> type != UNKNOW)
                .map(IMTerminalType::code)
                .collect(Collectors.toList());
    }

    public Integer code() {
        return this.code;
    }
}
