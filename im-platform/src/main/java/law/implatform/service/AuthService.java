package law.implatform.service;

import law.imcommon.util.R;
import law.implatform.model.dto.LoginDTO;
import law.implatform.model.dto.RegisterDTO;
import law.implatform.model.vo.LoginVO;

public interface AuthService {

    /**
     * 登录
     */
    LoginVO login(LoginDTO loginDto);

    /**
     * 注册
     */
    R<String> register(RegisterDTO registerDto);

    /**
     * 用refreshToken换取新 token
     *
     * @param refreshToken 刷新token
     * @return 登录token
     */
    LoginVO refreshToken(String refreshToken);
}
