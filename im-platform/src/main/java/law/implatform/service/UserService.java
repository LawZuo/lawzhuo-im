package law.implatform.service;

import law.implatform.entity.User;
import law.implatform.model.dto.ModifyPwdDTO;
import law.implatform.model.vo.OnlineTerminalVO;
import law.implatform.model.vo.UserVO;

import java.util.List;

public interface UserService {

    /**
     * 创建用户
     */
    Long create(User user);

    /**
     * 修改用户密码
     */
    void modifyPassword(ModifyPwdDTO dto);

    /**
     * 根据用户名查询用户
     */
    User findUserByUserName(String username);

    /**
     * 更新用户信息，好友昵称和群聊昵称等冗余信息也会更新
     */
    void update(UserVO vo);

    /**
     * 根据用户昵id查询用户以及在线状态
     */
    UserVO findUserById(Long id);

    /**
     * 根据用户昵称查询用户，最多返回20条数据
     */
    List<UserVO> search(String name);

    /**
     * 获取用户在线的终端类型
     */
    List<OnlineTerminalVO> getOnlineTerminals(String userIds);
}
