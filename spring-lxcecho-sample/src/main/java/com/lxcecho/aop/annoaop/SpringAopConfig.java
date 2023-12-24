package com.lxcecho.aop.annoaop;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/24
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan("com.lxcecho.aop.annoaop")
public class SpringAopConfig {
}
