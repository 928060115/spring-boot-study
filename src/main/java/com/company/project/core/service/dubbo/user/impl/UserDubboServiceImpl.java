package com.company.project.core.service.dubbo.user.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.company.project.core.service.dubbo.user.UserDubboService;
import com.company.project.model.User;
import com.company.project.service.UserService;

import javax.annotation.Resource;

@Service(version = "1.0.0")
public class UserDubboServiceImpl implements UserDubboService {

    @Resource
    UserService userService;

    @Override
    public User details(Integer id) {
        return  userService.findById(id);
    }
}
