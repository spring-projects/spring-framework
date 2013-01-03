/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.annotation.configuration;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import test.beans.TestBean;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Integration tests for {@link ImportResource} support.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class ImportResourceTests {
	@Test
	public void testImportXml() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlConfig.class);
		assertTrue("did not contain java-declared bean", ctx.containsBean("javaDeclaredBean"));
		assertTrue("did not contain xml-declared bean", ctx.containsBean("xmlDeclaredBean"));
		TestBean tb = ctx.getBean("javaDeclaredBean", TestBean.class);
		assertEquals("myName", tb.getName());
	}

	@Configuration
	@ImportResource("classpath:org/springframework/context/annotation/configuration/ImportXmlConfig-context.xml")
	static class ImportXmlConfig {
		@Value("${name}")
		private String name;
		public @Bean TestBean javaDeclaredBean() {
			return new TestBean(this.name);
		}
	}

	@Ignore // TODO: SPR-6310
	@Test
	public void testImportXmlWithRelativePath() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlWithRelativePathConfig.class);
		assertTrue("did not contain java-declared bean", ctx.containsBean("javaDeclaredBean"));
		assertTrue("did not contain xml-declared bean", ctx.containsBean("xmlDeclaredBean"));
		TestBean tb = ctx.getBean("javaDeclaredBean", TestBean.class);
		assertEquals("myName", tb.getName());
	}

	@Configuration
	@ImportResource("ImportXmlConfig-context.xml")
	static class ImportXmlWithRelativePathConfig {
		public @Bean TestBean javaDeclaredBean() {
			return new TestBean("java.declared");
		}
	}

	@Ignore // TODO: SPR-6310
	@Test
	public void testImportXmlByConvention() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlByConventionConfig.class);
		assertTrue("context does not contain xml-declared bean", ctx.containsBean("xmlDeclaredBean"));
	}

	@Configuration
	//@ImportXml
	static class ImportXmlByConventionConfig {
	}

	@Test
	public void testImportXmlIsInheritedFromSuperclassDeclarations() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(FirstLevelSubConfig.class);
		assertTrue(ctx.containsBean("xmlDeclaredBean"));
	}

	@Test
	public void testImportXmlIsMergedFromSuperclassDeclarations() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SecondLevelSubConfig.class);
		assertTrue("failed to pick up second-level-declared XML bean", ctx.containsBean("secondLevelXmlDeclaredBean"));
		assertTrue("failed to pick up parent-declared XML bean", ctx.containsBean("xmlDeclaredBean"));
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

	@Test
	public void testImportXmlWithNamespaceConfig() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlWithAopNamespaceConfig.class);
		Object bean = ctx.getBean("proxiedXmlBean");
		assertTrue(AopUtils.isAopProxy(bean));
	}

	@Configuration
	@ImportResource("classpath:org/springframework/context/annotation/configuration/ImportXmlWithAopNamespace-context.xml")
	static class ImportXmlWithAopNamespaceConfig {
	}

	@Aspect
	static class AnAspect {
		@Before("execution(* test.beans.TestBean.*(..))")
		public void advice() { }
	}

	@Test
	public void testImportXmlWithAutowiredConfig() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlAutowiredConfig.class);
		String name = ctx.getBean("xmlBeanName", String.class);
		assertThat(name, equalTo("xml.declared"));
	}

	@Configuration
	@ImportResource(value="classpath:org/springframework/context/annotation/configuration/ImportXmlConfig-context.xml")
	static class ImportXmlAutowiredConfig {
		@Autowired TestBean xmlDeclaredBean;

		public @Bean String xmlBeanName() {
			return xmlDeclaredBean.getName();
		}
	}

	@Test
	public void testImportNonXmlResource() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportNonXmlResourceConfig.class);
		assertTrue(ctx.containsBean("propertiesDeclaredBean"));
	}

	@Configuration
	@ImportResource(value="classpath:org/springframework/context/annotation/configuration/ImportNonXmlResourceConfig-context.properties",
			reader=PropertiesBeanDefinitionReader.class)
	static class ImportNonXmlResourceConfig {
	}

	@Ignore // TODO: SPR-6327
	@Test
	public void testImportDifferentResourceTypes() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SubResourceConfig.class);
		assertTrue(ctx.containsBean("propertiesDeclaredBean"));
		assertTrue(ctx.containsBean("xmlDeclaredBean"));
	}

	@Configuration
	@ImportResource(value="classpath:org/springframework/context/annotation/configuration/ImportXmlConfig-context.xml",
			reader=XmlBeanDefinitionReader.class)
	static class SubResourceConfig extends ImportNonXmlResourceConfig {
	}
}
