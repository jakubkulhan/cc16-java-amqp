package cz.codecamp;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AmqpApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AmqpApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.run(args);
    }
}
