package law.implatform.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import law.imcommon.util.R;
import law.implatform.entity.User;
import law.implatform.model.dto.ModifyPwdDTO;
import law.implatform.model.vo.OnlineTerminalVO;
import law.implatform.model.vo.UserVO;
import law.implatform.util.SecurityUtils;
import law.implatform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 修改用户密码
     */
    @PutMapping("/update-pwd")
    public R updatePwd(@RequestBody ModifyPwdDTO dto) {
        userService.modifyPassword(dto);
        return R.success();
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/self")
    public R<UserVO> self() {
        // 路径里没有 {username}，当前用户直接取自 token
        UserVO vo = userService.findUserById(SecurityUtils.getUserId());
        return R.success(vo);
    }

    /**
     * 判断用户哪个终端在线
     * 返回在线的用户id的终端集合
     */
    @GetMapping("/terminal/online")
    public R<List<OnlineTerminalVO>> getOnlineTerminal(@NotNull @RequestParam("userIds") String userIds) {
        return R.success(userService.getOnlineTerminals(userIds));
    }

    /**
     * 根据id查找用户
     */
    @GetMapping("/find/{id}")
    public R<UserVO> findById(@NotNull @PathVariable("id") Long id) {
        return R.success(userService.findUserById(id));
    }

    /**
     * 根据userName查找用户
     */
    @GetMapping("/find/username/{username}")
    public R<UserVO> findById(@NotNull @PathVariable("username") String username) {
        User user = userService.findUserByUserName(username);
        if (user == null) {
            return R.fail("用户不存在");
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return R.success(vo);
    }

    /**
     * 修改用户信息
     */
    @PutMapping("/update")
    public R update(@Valid @RequestBody UserVO vo) {
        userService.update(vo);
        return R.success();
    }

    /**
     * 查找用户
     */
    @GetMapping("/search")
    public R<List<UserVO>> search(@RequestParam String name) {
        return R.success(userService.search(name));
    }
}
