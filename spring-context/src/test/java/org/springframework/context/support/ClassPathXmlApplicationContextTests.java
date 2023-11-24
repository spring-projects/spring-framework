/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.context.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.ResourceTestBean;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
public class ClassPathXmlApplicationContextTests {

	private static final String PATH = "/org/springframework/context/support/";
	private static final String RESOURCE_CONTEXT = PATH + "ClassPathXmlApplicationContextTests-resource.xml";
	private static final String CONTEXT_WILDCARD = PATH + "test/context*.xml";
	private static final String CONTEXT_A = "test/contextA.xml";
	private static final String CONTEXT_B = "test/contextB.xml";
	private static final String CONTEXT_C = "test/contextC.xml";
	private static final String FQ_CONTEXT_A = PATH + CONTEXT_A;
	private static final String FQ_CONTEXT_B = PATH + CONTEXT_B;
	private static final String FQ_CONTEXT_C = PATH + CONTEXT_C;
	private static final String SIMPLE_CONTEXT = "simpleContext.xml";
	private static final String FQ_SIMPLE_CONTEXT = PATH + "simpleContext.xml";
	private static final String FQ_ALIASED_CONTEXT_C = PATH + "test/aliased-contextC.xml";
	private static final String INVALID_VALUE_TYPE_CONTEXT = PATH + "invalidValueType.xml";
	private static final String CHILD_WITH_PROXY_CONTEXT = PATH + "childWithProxy.xml";
	private static final String INVALID_CLASS_CONTEXT = "invalidClass.xml";
	private static final String CLASS_WITH_PLACEHOLDER_CONTEXT = "classWithPlaceholder.xml";
	private static final String ALIAS_THAT_OVERRIDES_PARENT_CONTEXT = PATH + "aliasThatOverridesParent.xml";
	private static final String ALIAS_FOR_PARENT_CONTEXT = PATH + "aliasForParent.xml";
	private static final String TEST_PROPERTIES = "test.properties";


	@Test
	void singleConfigLocation() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(FQ_SIMPLE_CONTEXT);
		assertThat(ctx.containsBean("someMessageSource")).isTrue();
		ctx.close();
	}

	@Test
	void multipleConfigLocations() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				FQ_CONTEXT_B, FQ_CONTEXT_C, FQ_CONTEXT_A);
		assertThat(ctx.containsBean("service")).isTrue();
		assertThat(ctx.containsBean("logicOne")).isTrue();
		assertThat(ctx.containsBean("logicTwo")).isTrue();

		// re-refresh (after construction refresh)
		Service service = (Service) ctx.getBean("service");
		ctx.refresh();
		assertThat(service.isProperlyDestroyed()).isTrue();

		// regular close call
		service = (Service) ctx.getBean("service");
		ctx.close();
		assertThat(service.isProperlyDestroyed()).isTrue();

		// re-activating and re-closing the context (SPR-13425)
		ctx.refresh();
		service = (Service) ctx.getBean("service");
		ctx.close();
		assertThat(service.isProperlyDestroyed()).isTrue();
	}

	@Test
	void configLocationPattern() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		assertThat(ctx.containsBean("service")).isTrue();
		assertThat(ctx.containsBean("logicOne")).isTrue();
		assertThat(ctx.containsBean("logicTwo")).isTrue();
		Service service = (Service) ctx.getBean("service");
		ctx.close();
		assertThat(service.isProperlyDestroyed()).isTrue();
	}

	@Test
	void singleConfigLocationWithClass() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(SIMPLE_CONTEXT, getClass());
		assertThat(ctx.containsBean("someMessageSource")).isTrue();
		ctx.close();
	}

	@Test
	void aliasWithPlaceholder() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				FQ_CONTEXT_B, FQ_ALIASED_CONTEXT_C, FQ_CONTEXT_A);
		assertThat(ctx.containsBean("service")).isTrue();
		assertThat(ctx.containsBean("logicOne")).isTrue();
		assertThat(ctx.containsBean("logicTwo")).isTrue();
		ctx.refresh();
		ctx.close();
	}

	@Test
	void contextWithInvalidValueType() throws IOException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {INVALID_VALUE_TYPE_CONTEXT}, false);
		assertThatExceptionOfType(BeanCreationException.class)
			.isThrownBy(context::refresh)
			.satisfies(ex -> {
				assertThat(ex.contains(TypeMismatchException.class)).isTrue();
				assertThat(ex.toString()).contains("someMessageSource", "useCodeAsDefaultMessage");
				checkExceptionFromInvalidValueType(ex);
				checkExceptionFromInvalidValueType(new ExceptionInInitializerError(ex));
				assertThat(context.isActive()).isFalse();
			});
		context.close();
	}

	private void checkExceptionFromInvalidValueType(Throwable ex) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ex.printStackTrace(new PrintStream(baos));
			String dump = FileCopyUtils.copyToString(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
			assertThat(dump).contains("someMessageSource");
			assertThat(dump).contains("useCodeAsDefaultMessage");
		}
		catch (IOException ioex) {
			throw new IllegalStateException(ioex);
		}
	}

	@Test
	void contextWithInvalidLazyClass() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(INVALID_CLASS_CONTEXT, getClass());
		assertThat(ctx.containsBean("someMessageSource")).isTrue();
		assertThatExceptionOfType(CannotLoadBeanClassException.class)
				.isThrownBy(() -> ctx.getBean("someMessageSource"))
				.withCauseExactlyInstanceOf(ClassNotFoundException.class);
		ctx.close();
	}

	@Test
	void contextWithClassNameThatContainsPlaceholder() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CLASS_WITH_PLACEHOLDER_CONTEXT, getClass());
		assertThat(ctx.containsBean("someMessageSource")).isTrue();
		assertThat(ctx.getBean("someMessageSource")).isInstanceOf(StaticMessageSource.class);
		ctx.close();
	}

	@Test
	void multipleConfigLocationsWithClass() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				new String[] {CONTEXT_B, CONTEXT_C, CONTEXT_A}, getClass());
		assertThat(ctx.containsBean("service")).isTrue();
		assertThat(ctx.containsBean("logicOne")).isTrue();
		assertThat(ctx.containsBean("logicTwo")).isTrue();
		ctx.close();
	}

	@Test
	void factoryBeanAndApplicationListener() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		ctx.getBeanFactory().registerSingleton("manualFBAAL", new FactoryBeanAndApplicationListener());
		assertThat(ctx.getBeansOfType(ApplicationListener.class)).hasSize(2);
		ctx.close();
	}

	@Test
	void messageSourceAware() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		MessageSource messageSource = (MessageSource) ctx.getBean("messageSource");
		Service service1 = (Service) ctx.getBean("service");
		assertThat(service1.getMessageSource()).isEqualTo(ctx);
		Service service2 = (Service) ctx.getBean("service2");
		assertThat(service2.getMessageSource()).isEqualTo(ctx);
		AutowiredService autowiredService1 = (AutowiredService) ctx.getBean("autowiredService");
		assertThat(autowiredService1.getMessageSource()).isEqualTo(messageSource);
		AutowiredService autowiredService2 = (AutowiredService) ctx.getBean("autowiredService2");
		assertThat(autowiredService2.getMessageSource()).isEqualTo(messageSource);
		ctx.close();
	}

	@Test
	void resourceArrayPropertyEditor() throws IOException {
		Resource contextA = new FileSystemResource(new ClassPathResource(FQ_CONTEXT_A).getFile());
		Resource contextB = new FileSystemResource(new ClassPathResource(FQ_CONTEXT_B).getFile());
		Resource contextC = new FileSystemResource(new ClassPathResource(FQ_CONTEXT_C).getFile());

		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		Service service = ctx.getBean("service", Service.class);
		assertThat(service.getResources()).containsExactlyInAnyOrder(contextA, contextB, contextC);
		assertThat(service.getResourceSet()).containsExactlyInAnyOrder(contextA, contextB, contextC);
		ctx.close();
	}

	@Test
	void childWithProxy() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(
				new String[] {CHILD_WITH_PROXY_CONTEXT}, ctx);
		assertThat(AopUtils.isAopProxy(child.getBean("assemblerOne"))).isTrue();
		assertThat(AopUtils.isAopProxy(child.getBean("assemblerTwo"))).isTrue();
		child.close();
		ctx.close();
	}

	@Test
	void aliasForParentContext() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(FQ_SIMPLE_CONTEXT);
		assertThat(ctx.containsBean("someMessageSource")).isTrue();

		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(
				new String[] {ALIAS_FOR_PARENT_CONTEXT}, ctx);
		assertThat(child.containsBean("someMessageSource")).isTrue();
		assertThat(child.containsBean("yourMessageSource")).isTrue();
		assertThat(child.containsBean("myMessageSource")).isTrue();
		assertThat(child.isSingleton("someMessageSource")).isTrue();
		assertThat(child.isSingleton("yourMessageSource")).isTrue();
		assertThat(child.isSingleton("myMessageSource")).isTrue();
		assertThat(child.getType("someMessageSource")).isEqualTo(StaticMessageSource.class);
		assertThat(child.getType("yourMessageSource")).isEqualTo(StaticMessageSource.class);
		assertThat(child.getType("myMessageSource")).isEqualTo(StaticMessageSource.class);

		Object someMs = child.getBean("someMessageSource");
		Object yourMs = child.getBean("yourMessageSource");
		Object myMs = child.getBean("myMessageSource");
		assertThat(yourMs).isSameAs(someMs);
		assertThat(myMs).isSameAs(someMs);

		String[] aliases = child.getAliases("someMessageSource");
		assertThat(aliases).hasSize(2);
		assertThat(aliases[0]).isEqualTo("myMessageSource");
		assertThat(aliases[1]).isEqualTo("yourMessageSource");
		aliases = child.getAliases("myMessageSource");
		assertThat(aliases).hasSize(2);
		assertThat(aliases[0]).isEqualTo("someMessageSource");
		assertThat(aliases[1]).isEqualTo("yourMessageSource");

		child.close();
		ctx.close();
	}

	@Test
	void aliasThatOverridesParent() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(FQ_SIMPLE_CONTEXT);
		Object someMs = ctx.getBean("someMessageSource");

		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(
				new String[] {ALIAS_THAT_OVERRIDES_PARENT_CONTEXT}, ctx);
		Object myMs = child.getBean("myMessageSource");
		Object someMs2 = child.getBean("someMessageSource");
		assertThat(someMs2).isSameAs(myMs);
		assertThat(someMs2).isNotSameAs(someMs);
		assertOneMessageSourceOnly(child, myMs);
	}

	@Test
	void aliasThatOverridesEarlierBean() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				FQ_SIMPLE_CONTEXT, ALIAS_THAT_OVERRIDES_PARENT_CONTEXT);
		Object myMs = ctx.getBean("myMessageSource");
		Object someMs2 = ctx.getBean("someMessageSource");
		assertThat(someMs2).isSameAs(myMs);
		assertOneMessageSourceOnly(ctx, myMs);
	}

	private void assertOneMessageSourceOnly(ClassPathXmlApplicationContext ctx, Object myMessageSource) {
		String[] beanNamesForType = ctx.getBeanNamesForType(StaticMessageSource.class);
		assertThat(beanNamesForType).hasSize(1);
		assertThat(beanNamesForType[0]).isEqualTo("myMessageSource");
		beanNamesForType = ctx.getBeanNamesForType(StaticMessageSource.class, true, true);
		assertThat(beanNamesForType).hasSize(1);
		assertThat(beanNamesForType[0]).isEqualTo("myMessageSource");
		beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(ctx, StaticMessageSource.class);
		assertThat(beanNamesForType).hasSize(1);
		assertThat(beanNamesForType[0]).isEqualTo("myMessageSource");
		beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(ctx, StaticMessageSource.class, true, true);
		assertThat(beanNamesForType).hasSize(1);
		assertThat(beanNamesForType[0]).isEqualTo("myMessageSource");

		Map<?, StaticMessageSource> beansOfType = ctx.getBeansOfType(StaticMessageSource.class);
		assertThat(beansOfType).hasSize(1);
		assertThat(beansOfType.values().iterator().next()).isSameAs(myMessageSource);
		beansOfType = ctx.getBeansOfType(StaticMessageSource.class, true, true);
		assertThat(beansOfType).hasSize(1);
		assertThat(beansOfType.values().iterator().next()).isSameAs(myMessageSource);
		beansOfType = BeanFactoryUtils.beansOfTypeIncludingAncestors(ctx, StaticMessageSource.class);
		assertThat(beansOfType).hasSize(1);
		assertThat(beansOfType.values().iterator().next()).isSameAs(myMessageSource);
		beansOfType = BeanFactoryUtils.beansOfTypeIncludingAncestors(ctx, StaticMessageSource.class, true, true);
		assertThat(beansOfType).hasSize(1);
		assertThat(beansOfType.values().iterator().next()).isSameAs(myMessageSource);
	}

	@Test
	void resourceAndInputStream() throws IOException {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(RESOURCE_CONTEXT) {
			@Override
			public Resource getResource(String location) {
				if (TEST_PROPERTIES.equals(location)) {
					return new ClassPathResource(TEST_PROPERTIES, ClassPathXmlApplicationContextTests.class);
				}
				return super.getResource(location);
			}
		};
		ResourceTestBean resource1 = (ResourceTestBean) ctx.getBean("resource1");
		ResourceTestBean resource2 = (ResourceTestBean) ctx.getBean("resource2");
		assertThat(resource1.getResource()).isInstanceOf(ClassPathResource.class);
		StringWriter writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource1.getResource().getInputStream()), writer);
		assertThat(writer.toString()).isEqualTo("contexttest");
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource1.getInputStream()), writer);
		assertThat(writer.toString()).isEqualTo("test");
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getResource().getInputStream()), writer);
		assertThat(writer.toString()).isEqualTo("contexttest");
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getInputStream()), writer);
		assertThat(writer.toString()).isEqualTo("test");
		ctx.close();
	}

	@Test
	void genericApplicationContextWithXmlBeanDefinitions() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_B, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_C, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_A, getClass()));
		ctx.refresh();
		assertThat(ctx.containsBean("service")).isTrue();
		assertThat(ctx.containsBean("logicOne")).isTrue();
		assertThat(ctx.containsBean("logicTwo")).isTrue();
		ctx.close();
	}

	@Test
	void genericApplicationContextWithXmlBeanDefinitionsAndClassLoaderNull() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setClassLoader(null);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_B, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_C, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_A, getClass()));
		ctx.refresh();
		assertThat(ctx.getId()).isEqualTo(ObjectUtils.identityToString(ctx));
		assertThat(ctx.getDisplayName()).isEqualTo(ObjectUtils.identityToString(ctx));
		assertThat(ctx.containsBean("service")).isTrue();
		assertThat(ctx.containsBean("logicOne")).isTrue();
		assertThat(ctx.containsBean("logicTwo")).isTrue();
		ctx.close();
	}

	@Test
	void genericApplicationContextWithXmlBeanDefinitionsAndSpecifiedId() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setId("testContext");
		ctx.setDisplayName("Test Context");
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_B, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_C, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_A, getClass()));
		ctx.refresh();
		assertThat(ctx.getId()).isEqualTo("testContext");
		assertThat(ctx.getDisplayName()).isEqualTo("Test Context");
		assertThat(ctx.containsBean("service")).isTrue();
		assertThat(ctx.containsBean("logicOne")).isTrue();
		assertThat(ctx.containsBean("logicTwo")).isTrue();
		ctx.close();
	}

}
