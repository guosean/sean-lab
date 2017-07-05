package com.sean.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;

/**
 * Created by guozhenbin on 2017/7/5.
 */
@Activate(group = {Constants.CONSUMER})
public class HystrixFilter implements Filter {

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("====HystrixFilter invoker:"+invoker+" invocation:"+invocation);
        return invoker.invoke(invocation);
    }

}
