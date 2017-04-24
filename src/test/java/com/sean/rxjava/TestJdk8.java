package com.sean.rxjava;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

/**
 * Created by guozhenbin on 2017/4/20.
 * 1. lambda @FunctionalInterface
 * 2. stream
 * 3. interface default
 * 4.
 *
 */
public class TestJdk8 {

    @Test
    public void testLambda(){
        List<String> list = Lists.newArrayList("c","d","a","e","b","c");
        list.stream().sorted((a,b) -> a.compareTo(b)).forEach(System.out::println);

    }

    @Test
    public void testFilter(){

        List<Integer> list = Lists.newArrayList(1,2,3,4,5);
        for(int i = 0; i< 1000; i++){
            list.add(i);
        }
        long start = System.currentTimeMillis();
        list.stream().filter(i -> i<10).forEach(i -> System.out.println(i));
        System.out.println("stream:"+(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        list.parallelStream().filter(i -> i<10).forEach(i -> System.out.println(i));
        System.out.println("pstream:"+(System.currentTimeMillis()-start));

    }

    @Test
    public void testMatchFind(){
        List<Integer> list = Lists.newArrayList(1,2,3,4,5);

        Assert.assertTrue(list.stream().allMatch(i -> i>0));
        Assert.assertFalse(list.stream().allMatch(i -> i<0));
        Assert.assertTrue(list.stream().anyMatch(i -> i>4));
        Assert.assertTrue(list.stream().noneMatch(i -> i<0));
        System.out.println(list.stream().findAny().get());
        Assert.assertEquals( 1, list.stream().findFirst().get().intValue());
    }

    @Test
    public void testSort(){
        List<Integer> list = Lists.newArrayList(1,3,2,5,5);
        Assert.assertEquals(5,list.stream().max(Comparator.comparingInt(Integer::intValue)).get().intValue());
        Assert.assertEquals(1,list.stream().min(Comparator.comparingInt(Integer::intValue)).get().intValue());
        list.stream().sorted(Comparator.comparingInt(Integer::intValue)).forEach(i -> System.out.println(i));
        list.stream().sorted(Comparator.comparingInt(Integer::intValue).reversed()).forEach(i -> System.out.println(i));
        list.stream().distinct().forEach(System.out::println);
    }

    @Test
    public void testMapReduce(){
        List<Integer> list = Lists.newArrayList(1,3,2,5,5);
        list.stream().map(t -> t*2).forEach(System.out::println);
        System.out.println(list.stream().map(t -> t*2).reduce((a,b) -> a+b).get().intValue());
        System.out.println(list.parallelStream().map(t -> t*2).reduce((a,b) -> a+b).get().intValue());
    }

}
