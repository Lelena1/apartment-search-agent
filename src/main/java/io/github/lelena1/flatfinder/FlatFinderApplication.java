package io.github.lelena1.flatfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FlatFinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlatFinderApplication.class, args);
    }
}
