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

package org.springframework.aop.scope;

import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.TestBeanRegistrationsAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import org.springframework.core.ResolvableType;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;

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

	private final DefaultListableBeanFactory beanFactory;

	private final TestBeanRegistrationsAotProcessor processor;

	private final TestGenerationContext generationContext;

	private final MockBeanFactoryInitializationCode beanFactoryInitializationCode;


	ScopedProxyBeanRegistrationAotProcessorTests() {
		this.beanFactory = new DefaultListableBeanFactory();
		this.processor = new TestBeanRegistrationsAotProcessor();
		this.generationContext = new TestGenerationContext();
		this.beanFactoryInitializationCode = new MockBeanFactoryInitializationCode(this.generationContext);
	}


	@Test
	void scopedProxyBeanRegistrationAotProcessorIsRegistered() {
		assertThat(AotServices.factoriesAndBeans(this.beanFactory).load(BeanRegistrationAotProcessor.class))
				.anyMatch(ScopedProxyBeanRegistrationAotProcessor.class::isInstance);
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenNotScopedProxy() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(PropertiesFactoryBean.class).getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		compile((freshBeanFactory, compiled) -> {
			Object bean = freshBeanFactory.getBean("test");
			assertThat(bean).isInstanceOf(Properties.class);
		});
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenScopedProxyWithoutTargetBeanName() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(ScopedProxyFactoryBean.class).getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		compile((freshBeanFactory, compiled) ->
				assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
					freshBeanFactory.getBean("test")).withMessageContaining("'targetBeanName' is required"));
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenScopedProxyWithInvalidTargetBeanName() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.addPropertyValue("targetBeanName", "testDoesNotExist")
				.getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		compile((freshBeanFactory, compiled) ->
				assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
						freshBeanFactory.getBean("test")).withMessageContaining("No bean named 'testDoesNotExist'"));
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
		compile((freshBeanFactory, compiled) -> {
			Object bean = freshBeanFactory.getBean("test");
			assertThat(bean).isInstanceOf(NumberHolder.class).isInstanceOf(AopInfrastructureBean.class);
		});
	}

	@SuppressWarnings("unchecked")
	private void compile(BiConsumer<DefaultListableBeanFactory, Compiled> result) {
		BeanFactoryInitializationAotContribution contribution = this.processor.processAheadOfTime(this.beanFactory);
		assertThat(contribution).isNotNull();
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		MethodReference methodReference = this.beanFactoryInitializationCode
				.getInitializers().get(0);
		this.beanFactoryInitializationCode.getTypeBuilder().set(type -> {
			CodeBlock methodInvocation = methodReference.toInvokeCodeBlock(
					ArgumentCodeGenerator.of(DefaultListableBeanFactory.class, "beanFactory"),
					this.beanFactoryInitializationCode.getClassName());
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(ParameterizedTypeName.get(Consumer.class, DefaultListableBeanFactory.class));
			type.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
					.addParameter(DefaultListableBeanFactory.class, "beanFactory")
					.addStatement(methodInvocation)
					.build());
		});
		this.generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(this.generationContext).compile(compiled -> {
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			freshBeanFactory.setBeanClassLoader(compiled.getClassLoader());
			compiled.getInstance(Consumer.class).accept(freshBeanFactory);
			result.accept(freshBeanFactory, compiled);
		});
	}

}
