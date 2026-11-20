/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
class BeanMethodMetadataTests {

	@Test
	void providesBeanMethodBeanDefinition() {
		AnnotationConfigApplicationContext context= new AnnotationConfigApplicationContext(Conf.class);
		BeanDefinition beanDefinition = context.getBeanDefinition("myBean");
		assertThat(beanDefinition).as("should provide AnnotatedBeanDefinition").isInstanceOf(AnnotatedBeanDefinition.class);
		Map<String, Object> annotationAttributes =
				((AnnotatedBeanDefinition) beanDefinition).getFactoryMethodMetadata().getAnnotationAttributes(MyAnnotation.class.getName());
		assertThat(annotationAttributes.get("value")).isEqualTo("test");
		context.close();
	}


	@Configuration
	static class Conf {

		@Bean
		@MyAnnotation("test")
		MyBean myBean() {
			return new MyBean();
		}
	}


	static class MyBean {
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface MyAnnotation {

		String value();
	}

}
