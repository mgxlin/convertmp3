package com.mgxlin.convertmp3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {"com.wancheli.module"})
public class Convertmp3Application {

    public static void main(String[] args) {
        SpringApplication.run(Convertmp3Application.class, args);
    }

}
