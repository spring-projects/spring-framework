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

package org.springframework.context.annotation.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures that @Configuration is supported properly as a meta-annotation.
 *
 * @author Chris Beams
 */
class ConfigurationMetaAnnotationTests {

	@Test
	void customConfigurationStereotype() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		assertThat(ctx.containsBean("customName")).isTrue();
		TestBean a = ctx.getBean("a", TestBean.class);
		TestBean b = ctx.getBean("b", TestBean.class);
		assertThat(b).isSameAs(a.getSpouse());
		ctx.close();
	}


	@TestConfiguration("customName")
	static class Config {
		@Bean
		TestBean a() {
			TestBean a = new TestBean();
			a.setSpouse(b());
			return a;
		}

		@Bean
		TestBean b() {
			return new TestBean();
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Configuration
	@interface TestConfiguration {

		@AliasFor(annotation = Configuration.class)
		String value() default "";
	}

}
