package com.badminton;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.badminton.mapper")
public class BadmintonApplication {

    public static void main(String[] args) {
        SpringApplication.run(BadmintonApplication.class, args);
    }
}
