package com.sean.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;

import java.util.Map;

/**
 * Created by guozhenbin on 2017/7/5.
 */
@Activate(group = {Constants.CONSUMER},before = "hystrix")
public class SeanFilter implements Filter{
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println(String.format("invoker:%s,invocation:%s",invoker,invocation));
        Map<String,String> attachs = invocation.getAttachments();
        System.out.println(attachs);
//        return invoker.invoke(invocation);
        RpcResult result = new RpcResult();
        result.setValue("filted");

//        return result;
        return invoker.invoke(invocation);
    }
}
