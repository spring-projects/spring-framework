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

package org.springframework.context.annotation.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.aspectj.ShouldBeConfiguredBySpring;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that @EnableSpringConfigured properly registers an
 * {@link org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect},
 * just as does {@code <context:spring-configured>}.
 *
 * @author Chris Beams
 * @since 3.1
 */
class AnnotationBeanConfigurerTests {

	@Test
	void injection() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
			ShouldBeConfiguredBySpring myObject = new ShouldBeConfiguredBySpring();
			assertThat(myObject.getName()).isEqualTo("Rod");
		}
	}


	@Configuration
	@ImportResource("org/springframework/beans/factory/aspectj/beanConfigurerTests-beans.xml")
	@EnableSpringConfigured
	static class Config {
	}

}
