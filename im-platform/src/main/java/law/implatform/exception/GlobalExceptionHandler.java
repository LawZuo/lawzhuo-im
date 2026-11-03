package law.implatform.exception;

import cn.hutool.json.JSONException;
import com.fasterxml.jackson.core.JsonProcessingException;
import law.imcommon.enums.CodeType;
import law.imcommon.util.R;
import law.implatform.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 全局异常处理
 *
 * <p>约定：
 * <ul>
 *   <li>业务异常（{@link GlobalException}）→ HTTP 200 + 异常自带的 code/message</li>
 *   <li>框架级错误（校验、JSON、404/405/415）→ HTTP 状态与 body.code 一致</li>
 * </ul>
 */
@ControllerAdvice
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public R handleException(Exception e) {
        if (e instanceof GlobalException) {
            GlobalException ex = (GlobalException) e;
            // token过期是正常情况,不打印
            if (ex.getCode() == null || ex.getCode() != ResultCode.INVALID_TOKEN.getCode()) {
                log.error("全局异常捕获:msg:{},log:{},{}", ex.getMessage(), e);
            }
            return R.fail(ex.getCode(), ex.getMessage());
        } else if (e instanceof UndeclaredThrowableException
                && e.getCause() instanceof GlobalException) {
            // 注意：cause 不一定是 GlobalException，直接强转会抛 ClassCastException
            GlobalException ex = (GlobalException) e.getCause();
            log.error("全局异常捕获:msg:{},log:{},{}", ex.getMessage(), e);
            return R.fail(ex.getCode(), ex.getMessage());
        } else {
            log.error("全局异常捕获:msg:{},{}", e.getMessage(), e);
            return R.fail(ResultCode.PROGRAM_ERROR.getCode(), ResultCode.PROGRAM_ERROR.getMsg());
        }
    }

    /**
     * 数据解析错误（Spring MVC 用 Jackson 解析请求体）
     */
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R handleMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("全局异常捕获:msg:{}", e.getMessage());
        Throwable cause = e.getCause();
        // Spring 的反序列化失败通常是 Jackson 的 JsonProcessingException
        if (cause instanceof JsonProcessingException || cause instanceof JSONException) {
            if (cause instanceof JsonProcessingException
                    && cause.getCause() instanceof DateTimeParseException) {
                return R.fail(CodeType.PARAM_ERROR.getCode(), "日期格式不正确");
            }
            return R.fail(CodeType.PARAM_ERROR.getCode(), "请求体格式错误");
        }
        return R.fail(CodeType.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    /**
     * 处理请求参数格式错误 @RequestBody上validate失败后抛出的异常
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R handleValidationExceptionHandler(MethodArgumentNotValidException exception) {
        BindingResult bindResult = exception.getBindingResult();
        String msg;
        if (bindResult.hasErrors()) {
            msg = bindResult.getAllErrors().get(0).getDefaultMessage();
            if (msg != null && msg.contains("NumberFormatException")) {
                msg = "参数类型错误！";
            }
        } else {
            msg = "系统繁忙，请稍后重试...";
        }
        return R.fail(CodeType.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 表单参数绑定失败
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R handleBindException(BindException e) {
        List<ObjectError> errors = e.getAllErrors();
        ObjectError error = errors.get(0);
        String errorMsg = error.getDefaultMessage();
        return R.fail(CodeType.PARAM_ERROR.getCode(), errorMsg);
    }

    /**
     * 参数类型不匹配（如 /api/user/find/abc 里的 abc 转不成 Long）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型错误: {}", e.getName());
        return R.fail(CodeType.PARAM_ERROR.getCode(), "参数类型错误: " + e.getName());
    }

    /**
     * 缺少必填查询参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少参数: {}", e.getParameterName());
        return R.fail(CodeType.PARAM_ERROR.getCode(), "缺少参数: " + e.getParameterName());
    }

    /**
     * 缺少必填请求头（如 refreshToken）
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("缺少请求头: {}", e.getHeaderName());
        return R.fail(CodeType.PARAM_ERROR.getCode(), "缺少请求头: " + e.getHeaderName());
    }

    /**
     * 路径不存在（Spring 6.1+ 静态资源处理器抛 NoResourceFoundException）
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R handleNotFound(Exception e) {
        log.warn("路径不存在: {}", e.getMessage());
        return R.fail(CodeType.NOT_FOUND.getCode(), CodeType.NOT_FOUND.getMessage());
    }

    /**
     * 请求方法不支持（如用 GET 调 POST 接口）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return R.fail(CodeType.METHOD_NOT_ALLOWED.getCode(),
                "请求方法不支持: " + e.getMethod());
    }

    /**
     * Content-Type 不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public R handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("Content-Type 不支持: {}", e.getContentType());
        return R.fail(CodeType.UNSUPPORTED_MEDIA_TYPE.getCode(),
                "Content-Type 不支持: " + e.getContentType());
    }
}
