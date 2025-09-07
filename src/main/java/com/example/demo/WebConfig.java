package com.example.demo; 

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
        	@Override
        	public void addCorsMappings(CorsRegistry registry) {
        	    registry.addMapping("/api/files/**")
        	            .allowedOrigins("http://localhost:3000")  // Or "*" to allow all origins
        	            .allowedMethods("GET", "POST", "DELETE")  // Allow all needed methods
        	            .allowedHeaders("*")
        	            .allowCredentials(true);
        	}

        };
    }
}

