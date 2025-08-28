package com.yourapp.dashboard.productivity_dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ProductivityDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductivityDashboardApplication.class, args);
    }

}
