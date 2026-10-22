package unichope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration")
public class UnichopeApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnichopeApplication.class, args);
    }
}
