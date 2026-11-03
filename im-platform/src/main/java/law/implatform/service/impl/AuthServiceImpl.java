package law.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import law.imcommon.contant.IMRedisKey;
import law.imcommon.jwt.JwtProperties;
import law.imcommon.jwt.JwtService;
import law.imcommon.model.UserSession;
import law.imcommon.util.R;
import law.implatform.entity.User;
import law.implatform.enums.ResultCode;
import law.implatform.exception.GlobalException;
import law.implatform.model.dto.LoginDTO;
import law.implatform.model.dto.RegisterDTO;
import law.implatform.model.vo.LoginVO;
import law.implatform.model.vo.UserVO;
import law.implatform.service.AuthService;
import law.implatform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginVO login(LoginDTO loginDto) {
        log.info("用户【{}】登录", loginDto.getUsername());
        User user = userService.findUserByUserName(loginDto.getUsername());
        if (user == null) {
            throw new GlobalException(ResultCode.PASSWOR_ERROR);
        }
        // 校验密码
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new GlobalException(ResultCode.PASSWOR_ERROR);
        }
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new GlobalException(String.format("您的账号因'%s'被管理员封禁,请联系客服!", user.getReason()));
        }

        UserSession userSession = new UserSession();
        BeanUtils.copyProperties(user, userSession);
        userSession.setUserId(user.getId());
        userSession.setTerminal(loginDto.getTerminal());

        return buildLoginVO(userSession);
    }

    @Override
    public R<String> register(RegisterDTO registerDto) {
        // 检查账号是否存在
        User userCheck = userService.findUserByUserName(registerDto.getUsername());
        if (userCheck != null) {
            return R.fail("账号已存在");
        }

        // 密码加密
        String pwd = registerDto.getPassword();
        if (StringUtils.hasText(pwd)) {
            pwd = passwordEncoder.encode(registerDto.getPassword());
        }

        // 创建用户
        User user = new User();
        user.setUserName(registerDto.getUsername());
        user.setPassword(pwd);
        user.setNickName(registerDto.getNickName());

        // 返回userId
        Long userId = userService.create(user);
        return R.success(userId.toString());
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 校验签名、过期时间、issuer，并确认它是 refresh token（不能拿 access token 换）
        UserSession session;
        try {
            session = jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException e) {
            log.warn("refreshToken 校验失败: {}", e.getMessage());
            throw new GlobalException(ResultCode.INVALID_TOKEN, "您的登录信息已过期，请重新登录");
        }

        Long userId = session.getUserId();
        UserVO user = userService.findUserById(userId);
        if (Objects.isNull(user)) {
            throw new GlobalException("用户不存在");
        }
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new GlobalException(String.format("您的账号因'%s'被管理员封禁,请联系客服!", user.getReason()));
        }
        // 清除残留的拒绝访问标记（解封后可能漏删）
        redisTemplate.delete(StrUtil.join(":", IMRedisKey.IM_USER_DENIED, user.getId()));

        return buildLoginVO(session);
    }

    private LoginVO buildLoginVO(UserSession session) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(jwtService.createAccessToken(session));
        vo.setAccessTokenExpiresIn((int) jwtProperties.getAccessToken().getExpireIn());
        vo.setRefreshToken(jwtService.createRefreshToken(session));
        vo.setRefreshTokenExpiresIn((int) jwtProperties.getRefreshToken().getExpireIn());
        return vo;
    }
}
