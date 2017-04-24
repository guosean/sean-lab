package com.sean.dubbo;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;

public class DubboConsumer {

    public static void main(String[] args) {
        ApplicationConfig config = new ApplicationConfig();
        config.setOwner("sean");
        config.setName("sean");
        RegistryConfig registry = new RegistryConfig();
        registry.setGroup("sean_dubbo_provider");
        registry.setProtocol("zookeeper");
        registry.setAddress("zk.dev.corp.qunar.com:2181");
        config.setRegistry(registry);
        ReferenceConfig<IHello> referenceConfig = new ReferenceConfig<IHello>();
        referenceConfig.setRegistry(registry);
        referenceConfig.setInterface("com.sean.dubbo.IHello");
        referenceConfig.setVersion("1.0.0");
        referenceConfig.setTimeout(5000);
        referenceConfig.setCheck(false);
        referenceConfig.setApplication(config);

        System.out.println(referenceConfig.get().sayHello());
    }
}
