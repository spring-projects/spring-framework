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

package org.springframework.aop.scope;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.aot.AotFactoriesLoader;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.TestBeanRegistrationsAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ScopedProxyBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
class ScopedProxyBeanRegistrationAotProcessorTests {

	private DefaultListableBeanFactory beanFactory;

	private TestBeanRegistrationsAotProcessor processor;

	private InMemoryGeneratedFiles generatedFiles;

	private DefaultGenerationContext generationContext;

	private MockBeanFactoryInitializationCode beanFactoryInitializationCode;

	@BeforeEach
	void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		this.processor = new TestBeanRegistrationsAotProcessor();
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new DefaultGenerationContext(this.generatedFiles);
		this.beanFactoryInitializationCode = new MockBeanFactoryInitializationCode();
	}

	@Test
	void scopedProxyBeanRegistrationAotProcessorIsRegistered() {
		assertThat(new AotFactoriesLoader(this.beanFactory).load(BeanRegistrationAotProcessor.class))
				.anyMatch(ScopedProxyBeanRegistrationAotProcessor.class::isInstance);
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenNotScopedProxy() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(PropertiesFactoryBean.class).getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		testCompile((freshBeanFactory, compiled) -> {
			Object bean = freshBeanFactory.getBean("test");
			assertThat(bean).isInstanceOf(Properties.class);
		});
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenScopedProxyWithoutTargetBeanName() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(ScopedProxyFactoryBean.class).getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		testCompile((freshBeanFactory,
				compiled) -> assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> freshBeanFactory.getBean("test"))
				.withMessageContaining("'targetBeanName' is required"));
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenScopedProxyWithInvalidTargetBeanName() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.addPropertyValue("targetBeanName", "testDoesNotExist")
				.getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		testCompile((freshBeanFactory,
				compiled) -> assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> freshBeanFactory.getBean("test"))
				.withMessageContaining("No bean named 'testDoesNotExist'"));
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenScopedProxyWithTargetBeanName() {
		RootBeanDefinition targetBean = new RootBeanDefinition();
		targetBean.setTargetType(
				ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
		targetBean.setScope("custom");
		this.beanFactory.registerBeanDefinition("numberHolder", targetBean);
		BeanDefinition scopedBean = BeanDefinitionBuilder
				.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.addPropertyValue("targetBeanName", "numberHolder").getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", scopedBean);
		testCompile((freshBeanFactory, compiled) -> {
			Object bean = freshBeanFactory.getBean("test");
			assertThat(bean).isNotNull().isInstanceOf(NumberHolder.class)
					.isInstanceOf(AopInfrastructureBean.class);
		});
	}

	private void testCompile(BiConsumer<DefaultListableBeanFactory, Compiled> result) {
		BeanFactoryInitializationAotContribution contribution = this.processor
				.processAheadOfTime(this.beanFactory);
		assertThat(contribution).isNotNull();
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		this.generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(this.generatedFiles).compile(compiled -> {
			MethodReference reference = this.beanFactoryInitializationCode
					.getInitializers().get(0);
			Object instance = compiled.getInstance(Object.class,
					reference.getDeclaringClass().toString());
			Method method = ReflectionUtils.findMethod(instance.getClass(),
					reference.getMethodName(), DefaultListableBeanFactory.class);
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			freshBeanFactory.setBeanClassLoader(compiled.getClassLoader());
			ReflectionUtils.invokeMethod(method, instance, freshBeanFactory);
			result.accept(freshBeanFactory, compiled);
		});
	}


	static class MockBeanFactoryInitializationCode implements BeanFactoryInitializationCode {

		private final GeneratedMethods generatedMethods = new GeneratedMethods();

		private final List<MethodReference> initializers = new ArrayList<>();

		@Override
		public MethodGenerator getMethodGenerator() {
			return this.generatedMethods;
		}

		@Override
		public void addInitializer(MethodReference methodReference) {
			this.initializers.add(methodReference);
		}

		List<MethodReference> getInitializers() {
			return this.initializers;
		}

	}

}
