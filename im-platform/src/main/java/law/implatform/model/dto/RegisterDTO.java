package law.implatform.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class RegisterDTO {

    @Length(max = 20, message = "昵称不能大于15字符")
    @NotBlank(message = "昵称不能为空")
    private String nickName;

    @Length(max = 20, message = "账号不能大于15字符")
    @NotBlank(message = "账号不能为空")
    private String username;

    @Length(min = 5, max = 20, message = "密码长度必须在5-20个字符之间")
    @NotEmpty(message = "密码不可为空")
    private String password;
}
