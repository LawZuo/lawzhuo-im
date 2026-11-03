package law.implatform.service.impl;

import law.imclient.IMClient;
import law.implatform.entity.User;
import law.implatform.exception.GlobalException;
import law.implatform.mapper.UserMapper;
import law.implatform.model.dto.ModifyPwdDTO;
import law.implatform.model.vo.OnlineTerminalVO;
import law.implatform.model.vo.UserVO;
import law.implatform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final IMClient imClient;

    @Override
    public Long create(User user) {
        if (user == null) {
            throw new GlobalException("用户不能为空");
        }
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public void modifyPassword(ModifyPwdDTO dto) {
        log.info("修改密码");
    }

    @Override
    public User findUserByUserName(String username) {
        return userMapper.selectUserByUsername(username);
    }

    @Override
    public void update(UserVO vo) {
        log.info("更新用户信息");
    }

    @Override
    public UserVO findUserById(Long id) {
        User user = userMapper.selectById(id);
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setOnline(imClient.isOnline(id));
        return vo;
    }

    @Override
    public List<UserVO> search(String name) {
        log.info("根据用户昵称查询用户");
        return List.of();
    }

    @Override
    public List<OnlineTerminalVO> getOnlineTerminals(String userIds) {
        log.info("获取用户在线的终端类型");
        return List.of();
    }
}
