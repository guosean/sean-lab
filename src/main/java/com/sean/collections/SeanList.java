package com.sean.collections;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by guozhenbin on 2017/5/10.
 */
public class SeanList {

    public static void main(String[] args) {
        List<String> arrayList = Lists.newArrayList("1","2","3");
        List<String> carrayList = Lists.newCopyOnWriteArrayList();
        carrayList.addAll(arrayList);
        for(String str:arrayList){
            arrayList.remove(str);
        }
        System.out.println(carrayList);

    }



}
