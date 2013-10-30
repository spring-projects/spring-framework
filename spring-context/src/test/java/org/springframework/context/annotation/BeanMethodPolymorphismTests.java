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

import java.lang.annotation.Inherited;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests regarding overloading and overriding of bean methods.
 * Related to SPR-6618.
 *
 * Bean-annotated methods should be able to be overridden, just as any regular
 * method. This is straightforward.
 *
 * Bean-annotated methods should be able to be overloaded, though supporting this
 * is more subtle. Essentially, it must be unambiguous to the container which bean
 * method to call.  A simple way to think about this is that no one Configuration
 * class may declare two bean methods with the same name.  In the case of inheritance,
 * the most specific subclass bean method will always be the one that is invoked.
 *
 * @author Chris Beams
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
public class BeanMethodPolymorphismTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


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
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SubConfig.class);
		assertThat(ctx.getBean(String.class), equalTo("overloaded5"));
	}

	@Test
	@Ignore
	public void beanMethodOverloadingWithInheritanceAndList() {
		// SPR-11025
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SubConfigWithList.class);
		assertThat(ctx.getBean(String.class), equalTo("overloaded5"));

	}
	
	@Test
	public void beanMethodOverloadingNotGreedyWithInheritance() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(NotGreedySubConfig.class);
		assertThat(ctx.getBean(String.class), equalTo("overloaded"));
	}
	static @Configuration class NotGreedySuperConfig {
		@Bean Integer anInt() { return 5; }
		@Bean String aString(Integer dependency) { return "super"; }
	}
	static @Configuration class NotGreedySubConfig extends NotGreedySuperConfig {
		
		@Bean String aString() { return "overloaded"; }
	}

	@Test
	public void beanMethodOverloadingNotBestMathcWithInheritance() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(NotBestMatchSubConfig.class);
		assertThat(ctx.getBean(String.class), equalTo("overloaded5"));
	}
	static @Configuration class NotBestMathcSuperConfig {
		@Bean String aString(Integer dependency) { return "super"; }
	}
	static @Configuration class NotBestMatchSubConfig extends NotBestMathcSuperConfig {
		@Bean Integer anInt() { return 5; }
		@Bean String aString(Number dependency) { return "overloaded"+dependency; }
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

	/**
	 * Tests that polymorphic Configuration classes need not explicitly redeclare the
	 * {@link Configuration} annotation. This respects the {@link Inherited} nature
	 * of the Configuration annotation, even though it's being detected via ASM.
	 */
	@Test
	public void beanMethodsDetectedOnSuperClass() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(Config.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		beanFactory.getBean("testBean", TestBean.class);
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

		@Bean
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

		@Bean
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
