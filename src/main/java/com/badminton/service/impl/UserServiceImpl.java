package com.badminton.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.badminton.common.BusinessException;
import com.badminton.dto.LoginRequest;
import com.badminton.entity.User;
import com.badminton.mapper.UserMapper;
import com.badminton.service.UserService;
import com.badminton.util.JwtUtil;
import com.badminton.vo.LoginVO;
import com.badminton.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final WxMaService wxMaService;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginRequest request) {
        try {
            // 调用微信接口获取openid
            WxMaJscode2SessionResult sessionResult = wxMaService.getUserService().getSessionInfo(request.getCode());
            String openid = sessionResult.getOpenid();

            // 查询或创建用户
            User user = getByOpenid(openid);
            if (user == null) {
                user = new User();
                user.setOpenid(openid);
                user.setLevel(5);
                user.setRole("user");
                userMapper.insert(user);
                log.info("新用户注册: openid={}", openid);
            }

            // 生成Token
            String token = jwtUtil.generateToken(user.getId(), user.getOpenid());

            // 构建响应
            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            loginVO.setUserInfo(toVO(user));

            return loginVO;
        } catch (Exception e) {
            log.error("微信登录失败", e);
            throw new BusinessException("登录失败: " + e.getMessage());
        }
    }

    @Override
    public User getByOpenid(String openid) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public void updateUserInfo(Long userId, String nickname, String avatar) {
        User user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        userMapper.updateById(user);
    }

    @Override
    public void updateAvatar(Long userId, String avatar) {
        User user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        user.setAvatar(avatar);
        userMapper.updateById(user);
    }

    @Override
    public void updateLevel(Long userId, Integer level) {
        User user = getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }

        if (level < 1 || level > 10) {
            throw new BusinessException("等级必须在1-10之间");
        }

        user.setLevel(level);
        userMapper.updateById(user);
    }

    @Override
    public UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setLevel(user.getLevel());
        return vo;
    }
}
