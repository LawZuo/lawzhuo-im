package law.implatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import law.implatform.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select(value = "SELECT * from im_user WHERE user_name = #{username}")
    User selectUserByUsername(@Param("username") String username);

    @Insert(value = "INSERT INTO im_user(user_name, password, nick_name) VALUES (#{username}, #{password}, #{nickName})")
    int createUser(
            @Param("username") String username,
            @Param("password") String password,
            @Param("nick_name") String nickName
    );
}
