package es.lesiones.liga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LesionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(LesionesApplication.class, args);
    }
}
