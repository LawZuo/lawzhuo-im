package law.implatform.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginDTO {

    // 登录终端 0:web 1:app 2:pc
    @Max(value = 2, message = "登录终端类型取值范围:0,2")
    @Min(value = 0, message = "登录终端类型取值范围:0,2")
    @NotNull(message = "登录终端类型不可为空")
    private Integer terminal;

    // 账号
    @NotBlank(message = "账号不能为空")
    private String username;

    // 密码
    @NotBlank(message = "密码不能为空")
    private String password;
}
