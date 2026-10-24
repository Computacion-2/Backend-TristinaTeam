package com.icesi.cuidapetproyect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class CuidaPetProyectApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CuidaPetProyectApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(CuidaPetProyectApplication.class, args);
    }

}
