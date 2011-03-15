/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.configuration.StubSpecification;

/**
 * Tests ensuring that @Feature methods can accept @Value-annoted
 * parameters, particularly String types. SPR-7974 revealed this
 * was failing due to attempting to proxy objects of type String,
 * which cannot be done.
 *
 * @author Chris Beams
 */
public class FeatureMethodValueInjectionTests {

	@Test
	public void control() {
		System.setProperty("foo", "bar");
		System.setProperty("num", "2");
		Config config = new AnnotationConfigApplicationContext(Config.class).getBean(Config.class);
		System.clearProperty("foo");
		System.clearProperty("num");
		assertThat(config.foo, is("bar"));
		assertThat(config.num, is(2));
	}

	@Test
	public void spelValueInjection() {
		System.setProperty("foo", "bar");
		new AnnotationConfigApplicationContext(SpelValueInjectionFeatureConfig.class);
		System.clearProperty("foo");
	}

	@Test
	public void spelIntValueInjection() {
		System.setProperty("num", "5");
		new AnnotationConfigApplicationContext(SpelIntValueInjectionFeatureConfig.class);
		System.clearProperty("num");
	}

	@Test
	public void stringBeanInjection() {
		new AnnotationConfigApplicationContext(StringBeanConfig.class, StringBeanInjectionByTypeFeatureConfig.class);
	}

	@Test
	public void qualifiedStringBeanInjection() {
		new AnnotationConfigApplicationContext(StringBeanSubConfig.class, StringBeanInjectionByQualifierFeatureConfig.class);
	}


	@FeatureConfiguration
	static class SpelValueInjectionFeatureConfig {
		@Feature
		public StubSpecification feature(@Value("#{environment['foo']}") String foo) {
			return new StubSpecification();
		}
	}


	@FeatureConfiguration
	static class SpelIntValueInjectionFeatureConfig {
		@Feature
		public StubSpecification feature(@Value("#{environment['num']}") int num) {
			assertThat(num, is(5));
			return new StubSpecification();
		}
	}


	@Configuration
	static class StringBeanConfig {
		@Bean
		public String stringBean() {
			return "sb";
		}
	}


	@Configuration
	static class StringBeanSubConfig extends StringBeanConfig {
		@Bean
		public String stringBean2() {
			return "sb2";
		}
	}


	@FeatureConfiguration
	static class StringBeanInjectionByTypeFeatureConfig {
		@Feature
		public StubSpecification feature(String string) {
			assertThat(string, is("sb"));
			return new StubSpecification();
		}
	}


	@FeatureConfiguration
	static class StringBeanInjectionByQualifierFeatureConfig {
		@Feature
		public StubSpecification feature(@Qualifier("stringBean2") String string) {
			assertThat(string, is("sb2"));
			return new StubSpecification();
		}
	}


	@Configuration
	static class Config {
		@Value("#{environment['foo']}") String foo;
		@Value("#{environment['num']}") int num;
	}
}
