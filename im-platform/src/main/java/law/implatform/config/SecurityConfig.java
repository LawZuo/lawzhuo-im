package law.implatform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import law.imcommon.enums.CodeType;
import law.imcommon.util.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 鉴权：基于 OAuth2 资源服务器（JWT 校验由 im-common 的 JwtAutoConfiguration 提供）。
 */

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF（无状态 API 通常不需要）
                .csrf(csrf -> csrf.disable())
                // 2. 配置授权规则（注意顺序：越具体的放前面）
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/public/**").permitAll()
                        // 错误页必须放行：否则 400/404/405/500 的 error dispatch 会被拦成 403 空响应
                        .requestMatchers("/error").permitAll()
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // 3. 设置无状态会话（不创建 HttpSession）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 4. OAuth2 资源服务器：Authorization: Bearer <jwt> 由 JwtDecoder 校验
                //    （签名、exp、issuer、token_type=access 都在 JwtService 里校验）
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        // token 无效/过期也返回统一 JSON，而不是空响应
                        .authenticationEntryPoint((request, response, e) -> {
                            log.warn("token 校验失败 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
                            writeJson(response, HttpStatus.UNAUTHORIZED, R.fail(CodeType.UNAUTHORIZED));
                        })
                )
                // 5. 认证/鉴权失败也返回 JSON，而不是空 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            log.warn("未认证访问 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
                            writeJson(response, HttpStatus.UNAUTHORIZED, R.fail(CodeType.UNAUTHORIZED));
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            log.warn("无权访问 {} {}", request.getMethod(), request.getRequestURI());
                            writeJson(response, HttpStatus.FORBIDDEN, R.fail(CodeType.FORBIDDEN));
                        })
                );
        return http.build();
    }

    /**
     * 目前 token 里没有 scope/role，先把权限集合置空，
     * 保留 sub（userId）作为 principal 的 name。
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthorityPrefix("");
        authorities.setAuthoritiesClaimName("scope");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, R<?> body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
