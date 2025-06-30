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

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.testfixture.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ImportResource @ImportResource} support.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class ImportResourceTests {

	@Test
	void importResource() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlConfig.class)) {
			assertThat(ctx.containsBean("javaDeclaredBean")).as("did not contain java-declared bean").isTrue();
			assertThat(ctx.containsBean("xmlDeclaredBean")).as("did not contain xml-declared bean").isTrue();
			TestBean tb = ctx.getBean("javaDeclaredBean", TestBean.class);
			assertThat(tb.getName()).isEqualTo("myName");
		}
	}

	@Test
	void importResourceIsInheritedFromSuperclassDeclarations() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(FirstLevelSubConfig.class)) {
			assertThat(ctx.containsBean("xmlDeclaredBean")).isTrue();
		}
	}

	@Test
	void importResourceIsMergedFromSuperclassDeclarations() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SecondLevelSubConfig.class)) {
			assertThat(ctx.containsBean("secondLevelXmlDeclaredBean")).as("failed to pick up second-level-declared XML bean").isTrue();
			assertThat(ctx.containsBean("xmlDeclaredBean")).as("failed to pick up parent-declared XML bean").isTrue();
		}
	}

	@Test
	void importResourceWithNamespaceConfig() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlWithAopNamespaceConfig.class)) {
			Object bean = ctx.getBean("proxiedXmlBean");
			assertThat(AopUtils.isAopProxy(bean)).isTrue();
		}
	}

	@Test
	void importResourceWithOtherConfigurationClass() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlWithConfigurationClass.class)) {
			assertThat(ctx.containsBean("javaDeclaredBean")).as("did not contain java-declared bean").isTrue();
			assertThat(ctx.containsBean("xmlDeclaredBean")).as("did not contain xml-declared bean").isTrue();
			TestBean tb = ctx.getBean("javaDeclaredBean", TestBean.class);
			assertThat(tb.getName()).isEqualTo("myName");
		}
	}

	@Test
	void importWithPlaceholder() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
			ctx.getEnvironment().getPropertySources().addFirst(new MockPropertySource("test").withProperty("test", "springframework"));
			ctx.register(ImportXmlConfig.class);
			ctx.refresh();
			assertThat(ctx.containsBean("xmlDeclaredBean")).as("did not contain xml-declared bean").isTrue();
		}
	}

	@Test
	void importResourceWithAutowiredConfig() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlAutowiredConfig.class)) {
			String name = ctx.getBean("xmlBeanName", String.class);
			assertThat(name).isEqualTo("xml.declared");
		}
	}

	@Test
	void importNonXmlResource() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportNonXmlResourceConfig.class)) {
			assertThat(ctx.containsBean("propertiesDeclaredBean")).isTrue();
		}
	}

	@Test
	void importResourceWithPrivateReader() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportWithPrivateReaderConfig.class)) {
			assertThat(ctx.containsBean("propertiesDeclaredBean")).isTrue();
		}
	}


	@Configuration
	@ImportResource("classpath:org/springframework/context/annotation/configuration/ImportXmlConfig-context.xml")
	static class ImportXmlConfig {

		@Value("${name}")
		private String name;

		@Bean public TestBean javaDeclaredBean() {
			return new TestBean(this.name);
		}
	}

	@Configuration
	@ImportResource("classpath:org/springframework/context/annotation/configuration/ImportXmlConfig-context.xml")
	static class BaseConfig {
	}

	@Configuration
	static class FirstLevelSubConfig extends BaseConfig {
	}

	@Configuration
	@ImportResource("classpath:org/springframework/context/annotation/configuration/SecondLevelSubConfig-context.xml")
	static class SecondLevelSubConfig extends BaseConfig {
	}

	@Configuration
	@ImportResource("classpath:org/springframework/context/annotation/configuration/ImportXmlWithAopNamespace-context.xml")
	static class ImportXmlWithAopNamespaceConfig {
	}

	@Aspect
	static class AnAspect {

		@Before("execution(* org.springframework.beans.testfixture.beans.TestBean.*(..))")
		public void advice() { }
	}

	@Configuration
	@ImportResource("classpath:org/springframework/context/annotation/configuration/ImportXmlWithConfigurationClass-context.xml")
	static class ImportXmlWithConfigurationClass {
	}

	@Configuration
	@ImportResource("classpath:org/springframework/context/annotation/configuration/ImportXmlConfig-context.xml")
	static class ImportXmlAutowiredConfig {

		@Autowired
		TestBean xmlDeclaredBean;

		@Bean
		public String xmlBeanName() {
			return xmlDeclaredBean.getName();
		}
	}

	@SuppressWarnings("deprecation")
	@Configuration
	@ImportResource(locations = "org/springframework/context/annotation/configuration/ImportNonXmlResourceConfig.properties",
			reader = org.springframework.beans.factory.support.PropertiesBeanDefinitionReader.class)
	static class ImportNonXmlResourceConfig {
	}

	@SuppressWarnings("deprecation")
	@Configuration
	@ImportResource(locations = "org/springframework/context/annotation/configuration/ImportNonXmlResourceConfig.properties",
			reader = PrivatePropertiesBeanDefinitionReader.class)
	static class ImportWithPrivateReaderConfig {
	}

	@SuppressWarnings("deprecation")
	private static class PrivatePropertiesBeanDefinitionReader
			extends org.springframework.beans.factory.support.PropertiesBeanDefinitionReader {

		PrivatePropertiesBeanDefinitionReader(BeanDefinitionRegistry registry) {
			super(registry);
		}
	}

}
