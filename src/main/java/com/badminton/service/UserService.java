package com.badminton.service;

import com.badminton.dto.LoginRequest;
import com.badminton.entity.User;
import com.badminton.vo.LoginVO;
import com.badminton.vo.UserVO;

public interface UserService {    LoginVO login(LoginRequest request);

    User getByOpenid(String openid);

    User getById(Long id);

    void updateUserInfo(Long userId, String nickname, String avatar);

    void updateAvatar(Long userId, String avatar);

    void updateLevel(Long userId, Integer level);

    UserVO toVO(User user);
}
