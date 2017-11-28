package com.company.project.core.service.dubbo.user;

import com.company.project.core.result.Result;
import com.company.project.model.User;

/**
 * 用户相关Dubbo接口
 */
public interface UserDubboService {

    /**
     * 根据id查询用户信息
     */
    User details(Integer id);
}
