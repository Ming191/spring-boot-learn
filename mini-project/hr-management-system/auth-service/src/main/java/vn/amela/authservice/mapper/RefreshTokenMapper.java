package vn.amela.authservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import vn.amela.authservice.entity.RefreshToken;

@Mapper
public interface RefreshTokenMapper {

    void insert(RefreshToken refreshToken);

    RefreshToken selectByTokenHash(String tokenHash);

    int revokeById(Long id);

    int revokeAllByUserId(Long userId);
}
