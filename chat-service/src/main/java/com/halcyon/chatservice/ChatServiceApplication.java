package com.halcyon.chatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(
        scanBasePackages = {
                "com.halcyon.chatservice",
                "com.halcyon.jwtlibrary"
        }
)
@EnableFeignClients(
        basePackages = "com.halcyon.clients"
)
public class ChatServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
