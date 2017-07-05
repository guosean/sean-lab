package com.sean.spi;

import java.util.ServiceLoader;

/**
 * Created by guozhenbin on 2017/7/5.
 */
public class SeanSpiService implements ISeanSpi{

    @Override
    public void hello() {
        System.out.println("sean spi service");
    }

    public static void main(String[] args) {
        ServiceLoader<ISeanSpi> seanSpis = ServiceLoader.load(ISeanSpi.class);
        for(ISeanSpi spi:seanSpis){
            spi.hello();
        }
    }
}
