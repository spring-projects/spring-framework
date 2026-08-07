package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TransitiveConfigurationTests {

    @Test
    void transitiveBeanUsageShouldFailInStrictMode() {
        System.setProperty("spring.strict.imports", "true");
        try {
            assertThrows(BeanDefinitionStoreException.class, () -> {
                new AnnotationConfigApplicationContext(ConfigA.class);
            });
        } finally {
            System.clearProperty("spring.strict.imports");
        }
    }
    
    @Configuration
    @Import(ConfigB.class)
    static class ConfigA {
        @Bean
        public String beanA(Integer beanC) {
            return "A depends on " + beanC;
        }
    }

    @Configuration
    @Import(ConfigC.class)
    static class ConfigB {
    }

    @Configuration
    static class ConfigC {
        @Bean
        public Integer beanC() {
            return 42;
        }
    }
}
