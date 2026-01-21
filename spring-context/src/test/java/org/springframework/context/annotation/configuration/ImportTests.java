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
import org.springframework.context.annotation.componentscan.ordered.SiblingImportingConfigA;
import org.springframework.context.annotation.componentscan.ordered.SiblingImportingConfigB;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link Import @Import} support.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Daeho Kwon
 */
class ImportTests {

	@Test
	void processImportsWithAsm() {
		int configClasses = 2;
		int beansInClasses = 2;
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ConfigurationWithImportAnnotation.class.getName()));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(configClasses + beansInClasses);
	}

	@Test
	void processImportsWithDoubleImports() {
		int configClasses = 3;
		int beansInClasses = 3;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfigurationWithImportAnnotation.class);
	}

	@Test
	void processImportsWithExplicitOverridingBefore() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), OtherConfiguration.class, ConfigurationWithImportAnnotation.class);
	}

	@Test
	void processImportsWithExplicitOverridingAfter() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class, OtherConfiguration.class);
	}

	@Test
	void importAnnotationWithTwoLevelRecursion() {
		int configClasses = 2;
		int beansInClasses = 3;
		assertBeanDefinitionCount((configClasses + beansInClasses), AppConfig.class);
	}

	@Test
	void importAnnotationWithThreeLevelRecursion() {
		int configClasses = 4;
		int beansInClasses = 5;
		assertBeanDefinitionCount(configClasses + beansInClasses, FirstLevel.class);
	}

	@Test
	void importAnnotationWithThreeLevelRecursionAndDoubleImport() {
		int configClasses = 5;
		int beansInClasses = 5;
		assertBeanDefinitionCount(configClasses + beansInClasses, FirstLevel.class, FirstLevelPlus.class);
	}

	@Test
	void importAnnotationWithMultipleArguments() {
		int configClasses = 3;
		int beansInClasses = 3;
		assertBeanDefinitionCount((configClasses + beansInClasses), WithMultipleArgumentsToImportAnnotation.class);
	}

	@Test
	void importAnnotationWithMultipleArgumentsResultingInOverriddenBeanDefinition() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.setAllowBeanDefinitionOverriding(true);
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(
				WithMultipleArgumentsThatWillCauseDuplication.class));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(4);
		assertThat(beanFactory.getBean("foo", ITestBean.class).getName()).isEqualTo("foo2");
	}

	@Test
	void importAnnotationOnInnerClasses() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), OuterConfig.InnerConfig.class);
	}

	@Test
	void importNonConfigurationAnnotationClass() {
		int configClasses = 2;
		int beansInClasses = 0;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigAnnotated.class);
	}

	/**
	 * Test that values supplied to @Configuration(value="...") are propagated as the
	 * bean name for the configuration class even in the case of inclusion via @Import
	 * or in the case of automatic registration via nesting
	 */
	@Test
	void reproSpr9023() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(B.class);
		assertThat(ctx.getBeanNamesForType(B.class)[0]).isEqualTo("config-b");
		assertThat(ctx.getBeanNamesForType(A.class)[0]).isEqualTo("config-a");
		ctx.close();
	}

	@Test
	void processImports() {
		int configClasses = 2;
		int beansInClasses = 2;
		assertBeanDefinitionCount((configClasses + beansInClasses), ConfigurationWithImportAnnotation.class);
	}

	/**
	 * An imported config must override a scanned one, thus bean definitions
	 * from the imported class is overridden by its importer.
	 */
	@Test  // gh-24643
	void importedConfigOverridesScanned() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setAllowBeanDefinitionOverriding(true);
		ctx.scan(SiblingImportingConfigA.class.getPackage().getName());
		ctx.refresh();

		assertThat(ctx.getBean("a-imports-b")).isEqualTo("valueFromA");
		assertThat(ctx.getBean("b-imports-a")).isEqualTo("valueFromBR");
		assertThat(ctx.getBeansOfType(SiblingImportingConfigA.class)).hasSize(1);
		assertThat(ctx.getBeansOfType(SiblingImportingConfigB.class)).hasSize(1);
	}

	@Test  // gh-34820
	void importAnnotationOnImplementedInterfaceIsRespected() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(InterfaceBasedConfig.class);

		assertThat(context.getBean(ImportedConfig.class)).isNotNull();
		assertThat(context.getBean(ImportedBean.class)).hasFieldOrPropertyWithValue("name", "imported");

		context.close();
	}

	@Test  // gh-34820
	void localImportShouldOverrideInterfaceImport() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(OverridingConfig.class);

		assertThat(context.getBean(ImportedConfig.class)).isNotNull();
		assertThat(context.getBean(OverridingImportedConfig.class)).isNotNull();
		assertThat(context.getBean(ImportedBean.class)).hasFieldOrPropertyWithValue("name", "from class");

		context.close();
	}


	private static DefaultListableBeanFactory processConfigurationClasses(Class<?>... classes) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.setAllowBeanDefinitionOverriding(false);
		for (Class<?> clazz : classes) {
			beanFactory.registerBeanDefinition(clazz.getSimpleName(), new RootBeanDefinition(clazz));
		}
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		return beanFactory;
	}

	private static void assertBeanDefinitionCount(int expectedCount, Class<?>... classes) {
		DefaultListableBeanFactory beanFactory = processConfigurationClasses(classes);
		assertThat(beanFactory.getBeanDefinitionCount()).isEqualTo(expectedCount);
		beanFactory.preInstantiateSingletons();
		for (Class<?> clazz : classes) {
			beanFactory.getBean(clazz);
		}
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

	@Configuration
	@Import(SecondLevel.class)
	static class FirstLevel {

		@Bean
		TestBean m() {
			return new TestBean();
		}
	}

	@Configuration
	@Import(ThirdLevel.class)
	static class FirstLevelPlus {
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

		static boolean initialized = false;

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

	@Configuration
	@Import(NonConfigAnnotated.class)
	static class ConfigAnnotated { }

	static class NonConfigAnnotated { }

	@Configuration("config-a")
	static class A { }

	@Configuration("config-b")
	@Import(A.class)
	static class B { }

	record ImportedBean(String name) {
	}

	@Configuration
	static class ImportedConfig {

		@Bean
		ImportedBean importedBean() {
			return new ImportedBean("imported");
		}
	}

	@Configuration
	static class OverridingImportedConfig {

		@Bean
		ImportedBean importedBean() {
			return new ImportedBean("from class");
		}
	}

	@Import(ImportedConfig.class)
	interface ConfigImportMarker {
	}

	@Configuration
	static class InterfaceBasedConfig implements ConfigImportMarker {
	}

	@Configuration
	@Import(OverridingImportedConfig.class)
	static class OverridingConfig implements ConfigImportMarker {
	}

}
