package com.ssh.bussi.controller;

import com.ssh.bussi.service.IUserService;
import com.ssh.core.annotation.MyAutowired;
import com.ssh.core.annotation.MyComponent;
import com.ssh.core.annotation.MyRequestMapping;

@MyComponent
@MyRequestMapping("/user")
public class UserController {

    @MyAutowired
    private IUserService userService;

    @MyRequestMapping("/list.do")
    public String list(){
        userService.test();
        System.out.println("===================");
        return "/list.jsp";
    }
}
