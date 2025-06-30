/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
