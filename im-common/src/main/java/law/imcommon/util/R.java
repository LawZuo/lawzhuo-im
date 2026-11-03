package law.imcommon.util;

import law.imcommon.enums.CodeType;

public class R<T> {

    /** 业务状态码，200 表示成功 */
    private Integer code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;
    /** 时间戳（可选，便于排查问题） */
    private Long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ---------- 成功 ----------

    public static <T> R<T> success() {
        return new R<>(CodeType.SUCCESS.getCode(), CodeType.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> success(T data) {
        return new R<>(CodeType.SUCCESS.getCode(), CodeType.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> success(String message, T data) {
        return new R<>(CodeType.SUCCESS.getCode(), message, data);
    }

    // ---------- 失败 ----------

    public static <T> R<T> fail(String message) {
        return new R<>(CodeType.FAIL.getCode(), message, null);
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(CodeType resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> fail(CodeType resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null);
    }

    // ---------- getter / setter ----------

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
