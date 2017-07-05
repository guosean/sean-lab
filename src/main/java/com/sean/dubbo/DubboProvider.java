package com.sean.dubbo;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;

public class DubboProvider {

    public static void main(String[] args) throws InterruptedException {
        ApplicationConfig config = new ApplicationConfig();
        config.setOwner("sean");
        config.setName("sean");
        RegistryConfig registry = new RegistryConfig();
        registry.setGroup("sean_dubbo_provider");
        registry.setProtocol("zookeeper");
        registry.setAddress("zk.dev.corp.qunar.com:2181");
        config.setRegistry(registry);

        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setApplication(config);
        providerConfig.setRegistry(registry);
        ServiceConfig<IHello> serviceConfig = new ServiceConfig<IHello>();
        serviceConfig.setRegistry(registry);
        serviceConfig.setVersion("1.0.0");
        serviceConfig.setTimeout(5000);
        serviceConfig.setProvider(providerConfig);
        serviceConfig.setInterface(IHello.class);
        serviceConfig.setRef(new IHello() {
            public String sayHello() {
                return "hello world";
            }
        });
        serviceConfig.export();

        Thread.sleep(100000);
    }

}
