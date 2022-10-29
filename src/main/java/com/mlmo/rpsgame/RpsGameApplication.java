package com.mlmo.rpsgame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class RpsGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpsGameApplication.class, args);
    }
}
