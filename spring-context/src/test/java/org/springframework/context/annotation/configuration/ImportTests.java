/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * System tests for {@link Import} annotation support.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class ImportTests {

	private DefaultListableBeanFactory processConfigurationClasses(Class<?>... classes) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		for (Class<?> clazz : classes) {
			beanFactory.registerBeanDefinition(clazz.getSimpleName(), new RootBeanDefinition(clazz));
		}
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		return beanFactory;
	}

	private void assertBeanDefinitionCount(int expectedCount, Class<?>... classes) {
		DefaultListableBeanFactory beanFactory = processConfigurationClasses(classes);
		assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(expectedCount);
		beanFactory.preInstantiateSingletons();
		for (Class<?> clazz : classes) {
			beanFactory.getBean(clazz);
		}

	}

	@Test
	public void testProcessImportsWithAsm() {
		int configClasses = 2;
		int beansInClasses = 2;
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ConfigurationWithImportAnnotation.class.getName()));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(configClasses + beansInClasses);
	}

	@Test
	public void testProcessImportsWithDoubleImports() {
		int configClasses = 3;
		int beansInClasses = 3;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfigurationWithImportAnnotation.class);
	}

	@Test
	public void testProcessImportsWithExplicitOverridingBefore() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), OtherConfiguration.class, ConfigurationWithImportAnnotation.class);
	}

	@Test
	public void testProcessImportsWithExplicitOverridingAfter() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfiguration.class);
	}

	@Configuration
	@Import(OtherConfiguration.class)
	static class ConfigurationWithImportAnnotation {
		@Bean
		public ITestBean one() {
			return new TestBean();
		}
	}

	@Configuration
	@Import(OtherConfiguration.class)
	static class OtherConfigurationWithImportAnnotation {
		@Bean
		public ITestBean two() {
			return new TestBean();
		}
	}

	@Configuration
	static class OtherConfiguration {
		@Bean
		public ITestBean three() {
			return new TestBean();
		}
	}

	// ------------------------------------------------------------------------

	@Test
	public void testImportAnnotationWithTwoLevelRecursion() {
		int configClasses = 2;
		int beansInClasses = 3;
		assertBeanDefinitionCount((configClasses + beansInClasses), AppConfig.class);
	}

	@Configuration
	@Import(DataSourceConfig.class)
	static class AppConfig {

		@Bean
		public ITestBean transferService() {
			return new TestBean(accountRepository());
		}

		@Bean
		public ITestBean accountRepository() {
			return new TestBean();
		}
	}

	@Configuration
	static class DataSourceConfig {
		@Bean
		public ITestBean dataSourceA() {
			return new TestBean();
		}
	}

	// ------------------------------------------------------------------------

	@Test
	public void testImportAnnotationWithThreeLevelRecursion() {
		int configClasses = 4;
		int beansInClasses = 5;
		assertBeanDefinitionCount(configClasses + beansInClasses, FirstLevel.class);
	}

	// ------------------------------------------------------------------------

	@Test
	public void testImportAnnotationWithMultipleArguments() {
		int configClasses = 3;
		int beansInClasses = 3;
		assertBeanDefinitionCount((configClasses + beansInClasses), WithMultipleArgumentsToImportAnnotation.class);
	}


	@Test
	public void testImportAnnotationWithMultipleArgumentsResultingInOverriddenBeanDefinition() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(
				WithMultipleArgumentsThatWillCauseDuplication.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(4);
		assertThat(beanFactory.getBean("foo", ITestBean.class).getName()).isEqualTo("foo2");
	}

	@Configuration
	@Import({Foo1.class, Foo2.class})
	static class WithMultipleArgumentsThatWillCauseDuplication {
	}

	@Configuration
	static class Foo1 {
		@Bean
		public ITestBean foo() {
			return new TestBean("foo1");
		}
	}

	@Configuration
	static class Foo2 {
		@Bean
		public ITestBean foo() {
			return new TestBean("foo2");
		}
	}

	// ------------------------------------------------------------------------

	@Test
	public void testImportAnnotationOnInnerClasses() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), OuterConfig.InnerConfig.class);
	}

	@Configuration
	static class OuterConfig {
		@Bean
		String whatev() {
			return "whatev";
		}

		@Configuration
		@Import(ExternalConfig.class)
		static class InnerConfig {
			@Bean
			public ITestBean innerBean() {
				return new TestBean();
			}
		}
	}

	@Configuration
	static class ExternalConfig {
		@Bean
		public ITestBean extBean() {
			return new TestBean();
		}
	}

	// ------------------------------------------------------------------------

	@Configuration
	@Import(SecondLevel.class)
	static class FirstLevel {
		@Bean
		public TestBean m() {
			return new TestBean();
		}
	}

	@Configuration
	@Import({ThirdLevel.class, InitBean.class})
	static class SecondLevel {
		@Bean
		public TestBean n() {
			return new TestBean();
		}
	}

	@Configuration
	@DependsOn("org.springframework.context.annotation.configuration.ImportTests$InitBean")
	static class ThirdLevel {
		public ThirdLevel() {
			assertThat(InitBean.initialized).isTrue();
		}

		@Bean
		public ITestBean thirdLevelA() {
			return new TestBean();
		}

		@Bean
		public ITestBean thirdLevelB() {
			return new TestBean();
		}

		@Bean
		public ITestBean thirdLevelC() {
			return new TestBean();
		}
	}

	static class InitBean {
		public static boolean initialized = false;

		public InitBean() {
			initialized = true;
		}
	}

	@Configuration
	@Import({LeftConfig.class, RightConfig.class})
	static class WithMultipleArgumentsToImportAnnotation {
		@Bean
		public TestBean m() {
			return new TestBean();
		}
	}

	@Configuration
	static class LeftConfig {
		@Bean
		public ITestBean left() {
			return new TestBean();
		}
	}

	@Configuration
	static class RightConfig {
		@Bean
		public ITestBean right() {
			return new TestBean();
		}
	}

	// ------------------------------------------------------------------------

	@Test
	public void testImportNonConfigurationAnnotationClass() {
		int configClasses = 2;
		int beansInClasses = 0;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigAnnotated.class);
	}

	@Configuration
	@Import(NonConfigAnnotated.class)
	static class ConfigAnnotated { }

	static class NonConfigAnnotated { }

	// ------------------------------------------------------------------------

	/**
	 * Test that values supplied to @Configuration(value="...") are propagated as the
	 * bean name for the configuration class even in the case of inclusion via @Import
	 * or in the case of automatic registration via nesting
	 */
	@Test
	public void reproSpr9023() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(B.class);
		ctx.refresh();
		System.out.println(ctx.getBeanFactory());
		assertThat(ctx.getBeanNamesForType(B.class)[0]).isEqualTo("config-b");
		assertThat(ctx.getBeanNamesForType(A.class)[0]).isEqualTo("config-a");
	}

	@Configuration("config-a")
	static class A { }

	@Configuration("config-b")
	@Import(A.class)
	static class B { }

	@Test
	public void testProcessImports() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class);
	}

}
