package law.imcommon.model;

import lombok.Data;

@Data
public class IMForceLogoutData {
    /**
     * 下线类型，见 {@link law.imcommon.enums.IMForceLogoutType}
     */
    private Integer type;

    /**
     * 原因说明（封禁时由管理端传入）
     */
    private String reason;
}
