package be.ephec.padelmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("be.ephec.padelmanager")
public class PadelManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PadelManagerApplication.class, args);
    }

}
