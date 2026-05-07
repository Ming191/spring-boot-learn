package com.amela.auth.mapper;

import com.amela.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    User findByUsername(String username);
    void save(User user);
    List<User> findAll();
}
