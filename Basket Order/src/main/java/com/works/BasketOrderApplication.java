package com.works;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class BasketOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasketOrderApplication.class, args);
    }

}
