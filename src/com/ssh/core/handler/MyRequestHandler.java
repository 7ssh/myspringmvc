package com.ssh.core.handler;

import java.lang.reflect.Method;

/**
 * 所有的请求地址都会有对应的Handler
 * Handler包含请求地址请求到的类以及类中的方法
 */
public class MyRequestHandler {

    private Object obj;// 对象

    private Method method;//  对象中具体方法

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "MyRequestHandler{" +
                "obj=" + obj +
                ", method=" + method +
                '}';
    }
}
