package com.sean.rxjava;

import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

/**
 * Created by guozhenbin on 2017/4/26.
 */
public class TestReflection {

    public static void main(String[] args) {
        Reflections reflections = new Reflections("com.sean.rxjava");
        reflections.getSubTypesOf(IRule.class).forEach(System.out::println);

    }

}

interface IRule{

}
class T1Rule implements IRule{

}
class T2Rule implements IRule{

}