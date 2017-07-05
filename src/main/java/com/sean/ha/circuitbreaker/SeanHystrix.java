package com.sean.ha.circuitbreaker;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class SeanHystrix {

    public static void main(String[] args) {
        HystrixCommand command = new HystrixCommand(HystrixCommandGroupKey.Factory.asKey("command")) {
            @Override
            protected Object run() throws Exception {
                return "testCommand";
            }
        };
        Object result = command.execute();
        System.out.println(result);
    }

}
