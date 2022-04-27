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
class ImportTests {

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
	void testProcessImportsWithAsm() {
		int configClasses = 2;
		int beansInClasses = 2;
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ConfigurationWithImportAnnotation.class.getName()));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(configClasses + beansInClasses);
	}

	@Test
	void testProcessImportsWithDoubleImports() {
		int configClasses = 3;
		int beansInClasses = 3;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfigurationWithImportAnnotation.class);
	}

	@Test
	void testProcessImportsWithExplicitOverridingBefore() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), OtherConfiguration.class, ConfigurationWithImportAnnotation.class);
	}

	@Test
	void testProcessImportsWithExplicitOverridingAfter() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfiguration.class);
	}

	@Configuration
	@Import(OtherConfiguration.class)
	static class ConfigurationWithImportAnnotation {
		@Bean
		ITestBean one() {
			return new TestBean();
		}
	}

	@Configuration
	@Import(OtherConfiguration.class)
	static class OtherConfigurationWithImportAnnotation {
		@Bean
		ITestBean two() {
			return new TestBean();
		}
	}

	@Configuration
	static class OtherConfiguration {
		@Bean
		ITestBean three() {
			return new TestBean();
		}
	}

	// ------------------------------------------------------------------------

	@Test
	void testImportAnnotationWithTwoLevelRecursion() {
		int configClasses = 2;
		int beansInClasses = 3;
		assertBeanDefinitionCount((configClasses + beansInClasses), AppConfig.class);
	}

	@Configuration
	@Import(DataSourceConfig.class)
	static class AppConfig {

		@Bean
		ITestBean transferService() {
			return new TestBean(accountRepository());
		}

		@Bean
		ITestBean accountRepository() {
			return new TestBean();
		}
	}

	@Configuration
	static class DataSourceConfig {
		@Bean
		ITestBean dataSourceA() {
			return new TestBean();
		}
	}

	// ------------------------------------------------------------------------

	@Test
	void testImportAnnotationWithThreeLevelRecursion() {
		int configClasses = 4;
		int beansInClasses = 5;
		assertBeanDefinitionCount(configClasses + beansInClasses, FirstLevel.class);
	}

	// ------------------------------------------------------------------------

	@Test
	void testImportAnnotationWithMultipleArguments() {
		int configClasses = 3;
		int beansInClasses = 3;
		assertBeanDefinitionCount((configClasses + beansInClasses), WithMultipleArgumentsToImportAnnotation.class);
	}


	@Test
	void testImportAnnotationWithMultipleArgumentsResultingInOverriddenBeanDefinition() {
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
		ITestBean foo() {
			return new TestBean("foo1");
		}
	}

	@Configuration
	static class Foo2 {
		@Bean
		ITestBean foo() {
			return new TestBean("foo2");
		}
	}

	// ------------------------------------------------------------------------

	@Test
	void testImportAnnotationOnInnerClasses() {
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
			ITestBean innerBean() {
				return new TestBean();
			}
		}
	}

	@Configuration
	static class ExternalConfig {
		@Bean
		ITestBean extBean() {
			return new TestBean();
		}
	}

	// ------------------------------------------------------------------------

	@Configuration
	@Import(SecondLevel.class)
	static class FirstLevel {
		@Bean
		TestBean m() {
			return new TestBean();
		}
	}

	@Configuration
	@Import({ThirdLevel.class, InitBean.class})
	static class SecondLevel {
		@Bean
		TestBean n() {
			return new TestBean();
		}
	}

	@Configuration
	@DependsOn("org.springframework.context.annotation.configuration.ImportTests$InitBean")
	static class ThirdLevel {
		ThirdLevel() {
			assertThat(InitBean.initialized).isTrue();
		}

		@Bean
		ITestBean thirdLevelA() {
			return new TestBean();
		}

		@Bean
		ITestBean thirdLevelB() {
			return new TestBean();
		}

		@Bean
		ITestBean thirdLevelC() {
			return new TestBean();
		}
	}

	static class InitBean {
		public static boolean initialized = false;

		InitBean() {
			initialized = true;
		}
	}

	@Configuration
	@Import({LeftConfig.class, RightConfig.class})
	static class WithMultipleArgumentsToImportAnnotation {
		@Bean
		TestBean m() {
			return new TestBean();
		}
	}

	@Configuration
	static class LeftConfig {
		@Bean
		ITestBean left() {
			return new TestBean();
		}
	}

	@Configuration
	static class RightConfig {
		@Bean
		ITestBean right() {
			return new TestBean();
		}
	}

	// ------------------------------------------------------------------------

	@Test
	void testImportNonConfigurationAnnotationClass() {
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
	void reproSpr9023() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(B.class);
		ctx.refresh();
		assertThat(ctx.getBeanNamesForType(B.class)[0]).isEqualTo("config-b");
		assertThat(ctx.getBeanNamesForType(A.class)[0]).isEqualTo("config-a");
		ctx.close();
	}

	@Configuration("config-a")
	static class A { }

	@Configuration("config-b")
	@Import(A.class)
	static class B { }

	@Test
	void testProcessImports() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class);
	}

}
