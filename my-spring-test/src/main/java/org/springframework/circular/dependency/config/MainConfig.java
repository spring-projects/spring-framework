package org.springframework.circular.dependency.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource(locations = "classpath:circular/dependncy/bean.xml")
public class MainConfig {
}
