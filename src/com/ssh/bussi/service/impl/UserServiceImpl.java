package com.ssh.bussi.service.impl;

import com.ssh.bussi.service.IUserService;
import com.ssh.core.annotation.MyComponent;

@MyComponent
public class UserServiceImpl implements IUserService{

    @Override
    public void test() {
        System.out.println("test()方法执行了");
    }
}
