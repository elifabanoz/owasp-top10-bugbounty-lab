package dev.bugbounty.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AdminLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminLabApplication.class, args);
    }
}
