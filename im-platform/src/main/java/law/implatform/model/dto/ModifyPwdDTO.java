package law.implatform.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 修改密码DTO
 */
@Data
public class ModifyPwdDTO {

    @NotEmpty(message = "旧用户密码不可为空")
    private String oldPassword;

    @NotEmpty(message = "新用户密码不可为空")
    private String newPassword;
}
