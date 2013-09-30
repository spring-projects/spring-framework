package org.springframework.context.annotation.configuration;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 *
 * Unit tests for SPR-10919, in which a bean definition is defined by a Java 8 default method annotated with @Bean.
 *
 * Author: Thomas Darimont
 *
 * @see https://jira.springsource.org/browse/SPR-10919
 */
public class JDK8BeanAnnotationOnDefaultMethodSupportTests {

    static class Dependency{}

    interface SubConfig {
        @Bean
        default Dependency dependency() {
            return new Dependency();
        }
    }

    @Configuration
    static class Config implements SubConfig{
    }

    @Test
    public void beanDefinitionOnDefaultMethodShouldBeRegisteredInBeanFactory(){

        AnnotationConfigApplicationContext ctxt = new AnnotationConfigApplicationContext(Config.class);
        assertThat(ctxt.containsBean("dependency"), is(true));
        assertThat(ctxt.getBean(Dependency.class), is(notNullValue()));
    }
}
