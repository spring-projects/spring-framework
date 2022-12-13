package com.ysj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@ComponentScan("com.ysj.beanfactory")
@Component("myappconfig")
public class AppConfig {
}
