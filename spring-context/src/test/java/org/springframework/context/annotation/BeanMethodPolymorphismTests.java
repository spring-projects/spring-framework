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

import org.junit.Test;

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
@SuppressWarnings("resource")
public class BeanMethodPolymorphismTests {

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
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithOverloading.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getBean(String.class), equalTo("regular"));
	}

	@Test
	public void beanMethodOverloadingWithoutInheritanceAndExtraDependency() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithOverloading.class);
		ctx.getDefaultListableBeanFactory().registerSingleton("anInt", 5);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getBean(String.class), equalTo("overloaded5"));
	}

	@Test
	public void beanMethodOverloadingWithAdditionalMetadata() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithOverloadingAndAdditionalMetadata.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertFalse(ctx.getDefaultListableBeanFactory().containsSingleton("aString"));
		assertThat(ctx.getBean(String.class), equalTo("regular"));
		assertTrue(ctx.getDefaultListableBeanFactory().containsSingleton("aString"));
	}

	@Test
	public void beanMethodOverloadingWithAdditionalMetadataButOtherMethodExecuted() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithOverloadingAndAdditionalMetadata.class);
		ctx.getDefaultListableBeanFactory().registerSingleton("anInt", 5);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertFalse(ctx.getDefaultListableBeanFactory().containsSingleton("aString"));
		assertThat(ctx.getBean(String.class), equalTo("overloaded5"));
		assertTrue(ctx.getDefaultListableBeanFactory().containsSingleton("aString"));
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
	static class ConfigWithOverloading {

		@Bean
		String aString() {
			return "regular";
		}

		@Bean
		String aString(Integer dependency) {
			return "overloaded" + dependency;
		}
	}


	@Configuration
	static class ConfigWithOverloadingAndAdditionalMetadata {

		@Bean @Lazy
		String aString() {
			return "regular";
		}

		@Bean
		String aString(Integer dependency) {
			return "overloaded" + dependency;
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
