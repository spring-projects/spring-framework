/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.interceptor.SimpleTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests regarding overloading and overriding of bean methods.
 * <p>Related to SPR-6618.
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
		assertThat(ctx.getBean("testBean", BaseTestBean.class)).isNotNull();
	}

	@Test
	public void beanMethodOverriding() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(OverridingConfig.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("testBean")).isFalse();
		assertThat(ctx.getBean("testBean", BaseTestBean.class).toString()).isEqualTo("overridden");
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("testBean")).isTrue();
	}

	@Test
	public void beanMethodOverridingOnASM() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(OverridingConfig.class.getName()));
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("testBean")).isFalse();
		assertThat(ctx.getBean("testBean", BaseTestBean.class).toString()).isEqualTo("overridden");
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("testBean")).isTrue();
	}

	@Test
	public void beanMethodOverridingWithNarrowedReturnType() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(NarrowedOverridingConfig.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("testBean")).isFalse();
		assertThat(ctx.getBean("testBean", BaseTestBean.class).toString()).isEqualTo("overridden");
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("testBean")).isTrue();
	}

	@Test
	public void beanMethodOverridingWithNarrowedReturnTypeOnASM() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(NarrowedOverridingConfig.class.getName()));
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("testBean")).isFalse();
		assertThat(ctx.getBean("testBean", BaseTestBean.class).toString()).isEqualTo("overridden");
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("testBean")).isTrue();
	}

	@Test
	public void beanMethodOverloadingWithoutInheritance() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithOverloading.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getBean(String.class)).isEqualTo("regular");
	}

	@Test
	public void beanMethodOverloadingWithoutInheritanceAndExtraDependency() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithOverloading.class);
		ctx.getDefaultListableBeanFactory().registerSingleton("anInt", 5);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
	}

	@Test
	public void beanMethodOverloadingWithAdditionalMetadata() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithOverloadingAndAdditionalMetadata.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("aString")).isFalse();
		assertThat(ctx.getBean(String.class)).isEqualTo("regular");
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("aString")).isTrue();
	}

	@Test
	public void beanMethodOverloadingWithAdditionalMetadataButOtherMethodExecuted() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithOverloadingAndAdditionalMetadata.class);
		ctx.getDefaultListableBeanFactory().registerSingleton("anInt", 5);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("aString")).isFalse();
		assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("aString")).isTrue();
	}

	@Test
	public void beanMethodOverloadingWithInheritance() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(SubConfig.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("aString")).isFalse();
		assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("aString")).isTrue();
	}

	// SPR-11025
	@Test
	public void beanMethodOverloadingWithInheritanceAndList() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(SubConfigWithList.class);
		ctx.setAllowBeanDefinitionOverriding(false);
		ctx.refresh();
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("aString")).isFalse();
		assertThat(ctx.getBean(String.class)).isEqualTo("overloaded5");
		assertThat(ctx.getDefaultListableBeanFactory().containsSingleton("aString")).isTrue();
	}

	/**
	 * When inheritance is not involved, it is still possible to override a bean method from
	 * the container's point of view. This is not strictly 'overloading' of a method per se,
	 * so it's referred to here as 'shadowing' to distinguish the difference.
	 */
	@Test
	public void beanMethodShadowing() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ShadowConfig.class);
		assertThat(ctx.getBean(String.class)).isEqualTo("shadow");
	}

	@Test
	public void beanMethodThroughAopProxy() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(Config.class);
		ctx.register(AnnotationAwareAspectJAutoProxyCreator.class);
		ctx.register(TestAdvisor.class);
		ctx.refresh();
		ctx.getBean("testBean", BaseTestBean.class);
	}


	static class BaseTestBean {
	}


	static class ExtendedTestBean extends BaseTestBean {
	}


	@Configuration
	static class BaseConfig {

		@Bean
		public BaseTestBean testBean() {
			return new BaseTestBean();
		}
	}


	@Configuration
	static class Config extends BaseConfig {
	}


	@Configuration
	static class OverridingConfig extends BaseConfig {

		@Bean @Lazy
		@Override
		public BaseTestBean testBean() {
			return new BaseTestBean() {
				@Override
				public String toString() {
					return "overridden";
				}
			};
		}
	}


	@Configuration
	static class NarrowedOverridingConfig extends BaseConfig {

		@Bean @Lazy
		@Override
		public ExtendedTestBean testBean() {
			return new ExtendedTestBean() {
				@Override
				public String toString() {
					return "overridden";
				}
			};
		}
	}


	@Configuration(enforceUniqueMethods = false)
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


	@Configuration(enforceUniqueMethods = false)
	static class ConfigWithOverloadingAndAdditionalMetadata {

		@Bean @Lazy
		String aString() {
			return "regular";
		}

		@Bean @Lazy
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


	@SuppressWarnings("serial")
	public static class TestAdvisor extends DefaultPointcutAdvisor {

		public TestAdvisor() {
			super(new SimpleTraceInterceptor());
		}
	}

}
