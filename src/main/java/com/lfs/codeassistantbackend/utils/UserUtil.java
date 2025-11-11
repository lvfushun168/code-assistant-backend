package com.lfs.codeassistantbackend.utils;

import com.lfs.codeassistantbackend.domain.dto.UserDto;

/**
 * 用户相关工具
 */
public class UserUtil {
    public static void clearAll(){
        userInfo.remove();
    }

    //用户基础信息
    private static final ThreadLocal<UserDto> userInfo = new ThreadLocal<>();
    public static UserDto getUserInfo() {
        return userInfo.get();
    }
    public static void setUserInfo(UserDto userDto) {
        userInfo.set(userDto);
    }
}
