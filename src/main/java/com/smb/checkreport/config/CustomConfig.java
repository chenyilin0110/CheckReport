package com.smb.checkreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "customer")
public class CustomConfig {
    private String code;
    private Integer machine;
    private String sync;
    private String ordersync;

    public String getOrdersync() {
        return ordersync;
    }

    public void setOrdersync(String ordersync) {
        this.ordersync = ordersync;
    }

    public String getSync() {
        return sync;
    }

    public void setSync(String sync) {
        this.sync = sync;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getMachine() {
        return machine;
    }

    public void setMachine(Integer machine) {
        this.machine = machine;
    }
}
