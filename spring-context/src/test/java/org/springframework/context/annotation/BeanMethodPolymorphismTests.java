/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests regarding overloading and overriding of bean methods.
 * Related to SPR-6618.
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
public class BeanMethodPolymorphismTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void beanMethodDetectedOnSuperClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		ctx.getBean("testBean", TestBean.class);
	}

	@Test
	public void beanMethodOverriding() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(OverridingConfig.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertFalse(ctx.getDefaultListableBeanFactory().containsSingleton("testBean"));
		assertEquals("overridden", ctx.getBean("testBean", TestBean.class).toString());
		assertTrue(ctx.getDefaultListableBeanFactory().containsSingleton("testBean"));
	}

	@Test
	public void beanMethodOverloadingWithoutInheritance() {

		@SuppressWarnings({ "hiding" })
		@Configuration class Config {
			@Bean String aString() { return "na"; }
			@Bean String aString(Integer dependency) { return "na"; }
		}

		this.thrown.expect(BeanDefinitionParsingException.class);
		this.thrown.expectMessage("overloaded @Bean methods named 'aString'");
		new AnnotationConfigApplicationContext(Config.class);
	}

	@Test
	public void beanMethodOverloadingWithInheritance() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(SubConfig.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertFalse(ctx.getDefaultListableBeanFactory().containsSingleton("aString"));
		assertThat(ctx.getBean(String.class), equalTo("overloaded5"));
		assertTrue(ctx.getDefaultListableBeanFactory().containsSingleton("aString"));
	}

	// SPR-11025
	@Test
	public void beanMethodOverloadingWithInheritanceAndList() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(SubConfigWithList.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertFalse(ctx.getDefaultListableBeanFactory().containsSingleton("aString"));
		assertThat(ctx.getBean(String.class), equalTo("overloaded5"));
		assertTrue(ctx.getDefaultListableBeanFactory().containsSingleton("aString"));
	}

	/**
	 * When inheritance is not involved, it is still possible to override a bean method from
	 * the container's point of view. This is not strictly 'overloading' of a method per se,
	 * so it's referred to here as 'shadowing' to distinguish the difference.
	 */
	@Test
	public void beanMethodShadowing() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ShadowConfig.class);
		assertThat(ctx.getBean(String.class), equalTo("shadow"));
	}


	@Configuration
	static class BaseConfig {

		@Bean
		public TestBean testBean() {
			return new TestBean();
		}
	}


	@Configuration
	static class Config extends BaseConfig {
	}


	@Configuration
	static class OverridingConfig extends BaseConfig {

		@Bean @Lazy
		@Override
		public TestBean testBean() {
			return new TestBean() {
				@Override
				public String toString() {
					return "overridden";
				}
			};
		}
	}


	@Configuration
	static class SuperConfig {

		@Bean
		String aString() {
			return "super";
		}
	}


	@Configuration
	static class SubConfig extends SuperConfig {

		@Bean
		Integer anInt() {
			return 5;
		}

		@Bean @Lazy
		String aString(Integer dependency) {
			return "overloaded" + dependency;
		}
	}


	@Configuration
	static class SubConfigWithList extends SuperConfig {

		@Bean
		Integer anInt() {
			return 5;
		}

		@Bean @Lazy
		String aString(List<Integer> dependency) {
			return "overloaded" + dependency.get(0);
		}
	}


	@Configuration
	@Import(SubConfig.class)
	static class ShadowConfig {

		@Bean
		String aString() {
			return "shadow";
		}
	}

}
