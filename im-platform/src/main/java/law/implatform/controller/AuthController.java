package law.implatform.controller;

import jakarta.validation.Valid;
import law.imcommon.util.R;
import law.implatform.model.dto.LoginDTO;
import law.implatform.model.dto.RegisterDTO;
import law.implatform.model.vo.LoginVO;
import law.implatform.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public R<LoginVO> login(
            @Valid @RequestBody LoginDTO loginDto
    ) {
        LoginVO vo = authService.login(loginDto);
        return R.success(vo);
    }

    /**
     * 注册
     */
    @PostMapping("register")
    public R<String> register(
            @Valid @RequestBody RegisterDTO registerDto
            ) {
        return authService.register(registerDto);
    }

    /**
     * 刷新token
     */
    @PutMapping("/refreshToken")
    public R refreshToken(@RequestHeader("refreshToken") String refreshToken) {
        LoginVO vo = authService.refreshToken(refreshToken);
        return R.success(vo);
    }

}
