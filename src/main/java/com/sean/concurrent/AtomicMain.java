package com.sean.concurrent;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class AtomicMain {

    private int value;
//    private static Unsafe unsafe = Unsafe.getUnsafe();
    private static final long offset = 0;
    /*static {
        try {
            unsafe.objectFieldOffset(AtomicMain.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }*/

    public long getOffset(){
        return offset;
    }

    public static void main(String[] args) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get("null");
            long current = System.currentTimeMillis();
            System.out.println(unsafe);
            LockSupport.parkNanos(field,10000000l);
            System.out.println(System.currentTimeMillis() - current);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e){
            e.printStackTrace();
        }

    }

}
