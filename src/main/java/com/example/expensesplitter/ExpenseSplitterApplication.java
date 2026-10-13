package com.example.expensesplitter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the application.
 *
 * @SpringBootApplication turns on three things at once:
 *  - component scanning (finds our @RestController, @Service, @Repository classes),
 *  - auto-configuration (sets up Tomcat, JPA, H2 ... based on the dependencies),
 *  - configuration support (this class can define @Bean methods).
 */
@SpringBootApplication
public class ExpenseSplitterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseSplitterApplication.class, args);
    }
}
