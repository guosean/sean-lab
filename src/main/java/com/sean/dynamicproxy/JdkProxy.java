package com.sean.dynamicproxy;

import sun.reflect.Reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by guozhenbin on 2017/5/25.
 */
public class JdkProxy {

    public static void main(String[] args) {
        TestInterface proxy = (TestInterface) Proxy.newProxyInstance(JdkProxy.class.getClassLoader(), new Class[]{TestInterface.class},new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                if(methodName.equals("name")){
                    return "sean";
                }else if(methodName.equals("age")){
                    return 18;
                }
                return null;
            }
        });

        System.out.println(proxy.name());

        System.out.println(System.getSecurityManager());
    }

}
