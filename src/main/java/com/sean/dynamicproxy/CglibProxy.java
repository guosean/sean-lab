package com.sean.dynamicproxy;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.FixedValue;

/**
 * Created by guozhenbin on 2017/5/25.
 */
public class CglibProxy {

    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(TestCg.class);
        enhancer.setCallback(new FixedValue() {
            @Override
            public Object loadObject() throws Exception {
                return "dgs";
            }
        });
        TestCg proxy = (TestCg) enhancer.create();
        System.out.println(proxy.getName());
        System.out.println(proxy.toString());
    }

    static class TestCg{
        public String getName(){
            return "name";
        }
        public void say(){
            System.out.println("say");
        }
        public int age(){
            return 18;
        }
    }
}
