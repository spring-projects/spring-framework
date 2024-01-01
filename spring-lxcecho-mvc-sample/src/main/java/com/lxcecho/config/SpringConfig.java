package com.lxcecho.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

/**
 * Spring 不扫描 controller 组件、AOP 咋实现的....?????
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2024/1/1
 */
@ComponentScan(value = "com.lxcecho", excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, value = Controller.class)
})
@Configuration
public class SpringConfig {
	// Spring 的父容器
}
