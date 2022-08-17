package com.ysj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@ComponentScan("com.ysj.bean")
@Configuration(value = "myAppConfig")
@Service
public class AppConfig {
}
