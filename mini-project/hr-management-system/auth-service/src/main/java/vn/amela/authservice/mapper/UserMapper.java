package vn.amela.authservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import vn.amela.authservice.entity.User;

@Mapper
public interface UserMapper {
    User selectByUserName(String username);

    User selectByEmail(String email);

    void insert(User user);

    User selectByUserNameOrEmail(String usernameOrEmail);
}
