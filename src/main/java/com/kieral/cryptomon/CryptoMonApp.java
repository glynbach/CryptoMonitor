package com.kieral.cryptomon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.kieral.cryptomon")
public class CryptoMonApp {

    public static void main(String[] args) {
        SpringApplication.run(CryptoMonApp.class, args);
    }

}
