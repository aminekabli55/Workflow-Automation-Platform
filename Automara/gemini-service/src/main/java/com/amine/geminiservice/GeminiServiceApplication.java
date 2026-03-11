package com.amine.geminiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GeminiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeminiServiceApplication.class, args);
    }

}
