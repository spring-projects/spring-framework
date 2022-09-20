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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.testfixture.context.generator.annotation.ImportAwareConfiguration;
import org.springframework.context.testfixture.context.generator.annotation.ImportConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConfigurationClassPostProcessor} AOT contributions.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ConfigurationClassPostProcessorAotContributionTests {

	private final TestGenerationContext generationContext = new TestGenerationContext();

	private final MockBeanFactoryInitializationCode beanFactoryInitializationCode =
			new MockBeanFactoryInitializationCode(this.generationContext);


	@Nested
	class ImportAwareTests {

		@Test
		void processAheadOfTimeWhenNoImportAwareConfigurationReturnsNull() {
			assertThat(getContribution(SimpleConfiguration.class)).isNull();
		}

		@Test
		void applyToWhenHasImportAwareConfigurationRegistersBeanPostProcessorWithMapEntry() {
			BeanFactoryInitializationAotContribution contribution = getContribution(ImportConfiguration.class);
			contribution.applyTo(generationContext, beanFactoryInitializationCode);
			compile((initializer, compiled) -> {
				GenericApplicationContext freshContext = new GenericApplicationContext();
				DefaultListableBeanFactory freshBeanFactory = freshContext.getDefaultListableBeanFactory();
				initializer.accept(freshBeanFactory);
				freshContext.refresh();
				assertThat(freshBeanFactory.getBeanPostProcessors()).filteredOn(ImportAwareAotBeanPostProcessor.class::isInstance)
						.singleElement().satisfies(postProcessor -> assertPostProcessorEntry(postProcessor, ImportAwareConfiguration.class,
								ImportConfiguration.class));
				freshContext.close();
			});
		}

		@Test
		void applyToWhenHasImportAwareConfigurationRegistersBeanPostProcessorAfterApplicationContextAwareProcessor() {
			BeanFactoryInitializationAotContribution contribution = getContribution(TestAwareCallbackConfiguration.class);
			contribution.applyTo(generationContext, beanFactoryInitializationCode);
			compile((initializer, compiled) -> {
				GenericApplicationContext freshContext = new GenericApplicationContext();
				DefaultListableBeanFactory freshBeanFactory = freshContext.getDefaultListableBeanFactory();
				initializer.accept(freshBeanFactory);
				freshContext.registerBean(TestAwareCallbackBean.class);
				freshContext.refresh();
				TestAwareCallbackBean bean = freshContext.getBean(TestAwareCallbackBean.class);
				assertThat(bean.instances).hasSize(2);
				assertThat(bean.instances.get(0)).isEqualTo(freshContext);
				assertThat(bean.instances.get(1)).isInstanceOfSatisfying(AnnotationMetadata.class, metadata ->
						assertThat(metadata.getClassName()).isEqualTo(TestAwareCallbackConfiguration.class.getName()));
				freshContext.close();
			});
		}

		@Test
		void applyToWhenHasImportAwareConfigurationRegistersBeanPostProcessorBeforeRegularBeanPostProcessor() {
			BeanFactoryInitializationAotContribution contribution = getContribution(
					TestImportAwareBeanPostProcessorConfiguration.class);
			contribution.applyTo(generationContext, beanFactoryInitializationCode);
			compile((initializer, compiled) -> {
				GenericApplicationContext freshContext = new GenericApplicationContext();
				DefaultListableBeanFactory freshBeanFactory = freshContext.getDefaultListableBeanFactory();
				initializer.accept(freshBeanFactory);
				freshBeanFactory.registerBeanDefinition(TestImportAwareBeanPostProcessor.class.getName(),
						new RootBeanDefinition(TestImportAwareBeanPostProcessor.class));
				RootBeanDefinition bd = new RootBeanDefinition(String.class);
				bd.setInstanceSupplier(() -> "test");
				freshBeanFactory.registerBeanDefinition("testProcessing", bd);
				freshContext.refresh();
				assertThat(freshContext.getBean("testProcessing")).isInstanceOfSatisfying(AnnotationMetadata.class, metadata ->
						assertThat(metadata.getClassName()).isEqualTo(TestImportAwareBeanPostProcessorConfiguration.class.getName())
				);
				freshContext.close();
			});
		}

		@Test
		void applyToWhenHasImportAwareConfigurationRegistersHints() {
			BeanFactoryInitializationAotContribution contribution = getContribution(ImportConfiguration.class);
			contribution.applyTo(generationContext, beanFactoryInitializationCode);
			assertThat(generationContext.getRuntimeHints().resources().resourcePatterns())
					.singleElement()
					.satisfies(resourceHint -> assertThat(resourceHint.getIncludes())
							.map(ResourcePatternHint::getPattern)
							.containsOnly("org/springframework/context/testfixture/context/generator/annotation/"
									+ "ImportConfiguration.class"));
		}

		@SuppressWarnings("unchecked")
		private void compile(BiConsumer<Consumer<DefaultListableBeanFactory>, Compiled> result) {
			MethodReference methodReference = beanFactoryInitializationCode.getInitializers().get(0);
			beanFactoryInitializationCode.getTypeBuilder().set(type -> {
				CodeBlock methodInvocation = methodReference.toInvokeCodeBlock(
						ArgumentCodeGenerator.of(DefaultListableBeanFactory.class, "beanFactory"),
						beanFactoryInitializationCode.getClassName());
				type.addModifiers(Modifier.PUBLIC);
				type.addSuperinterface(ParameterizedTypeName.get(Consumer.class, DefaultListableBeanFactory.class));
				type.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
						.addParameter(DefaultListableBeanFactory.class, "beanFactory")
						.addStatement(methodInvocation)
						.build());
			});
			generationContext.writeGeneratedContent();
			TestCompiler.forSystem().withFiles(generationContext.getGeneratedFiles()).compile(compiled ->
					result.accept(compiled.getInstance(Consumer.class), compiled));
		}

		@Configuration(proxyBeanMethods = false)
		@Import(TestAwareCallbackBean.class)
		static class TestAwareCallbackConfiguration {
		}

		static class TestAwareCallbackBean implements ImportAware, ApplicationContextAware {

			private final List<Object> instances = new ArrayList<>();

			@Override
			public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
				this.instances.add(applicationContext);
			}

			@Override
			public void setImportMetadata(AnnotationMetadata importMetadata) {
				this.instances.add(importMetadata);
			}

		}

		@Configuration(proxyBeanMethods = false)
		@Import(TestImportAwareBeanPostProcessor.class)
		static class TestImportAwareBeanPostProcessorConfiguration {
		}

		static class TestImportAwareBeanPostProcessor implements BeanPostProcessor, ImportAware,
				Ordered, InitializingBean {

			private AnnotationMetadata metadata;

			@Override
			public void setImportMetadata(AnnotationMetadata importMetadata) {
				this.metadata = importMetadata;
			}

			@Nullable
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				if (beanName.equals("testProcessing")) {
					return this.metadata;
				}
				return bean;
			}

			@Override
			public int getOrder() {
				return Ordered.HIGHEST_PRECEDENCE;
			}

			@Override
			public void afterPropertiesSet() throws Exception {
				Assert.notNull(this.metadata, "Metadata was not injected");
			}

		}
	}

	@Nested
	class PropertySourceTests {

		@Test
		void applyToWhenHasPropertySourceInvokePropertySourceProcessor() {
			BeanFactoryInitializationAotContribution contribution = getContribution(
					PropertySourceConfiguration.class);
			contribution.applyTo(generationContext, beanFactoryInitializationCode);
			compile((initializer, compiled) -> {
				GenericApplicationContext freshContext = new GenericApplicationContext();
				ConfigurableEnvironment environment = freshContext.getEnvironment();
				assertThat(environment.containsProperty("from.p1")).isFalse();
				initializer.accept(freshContext);
				assertThat(environment.containsProperty("from.p1")).isTrue();
				assertThat(environment.getProperty("from.p1")).isEqualTo("p1Value");
				freshContext.close();
			});
		}

		@Test
		void applyToWhenHasPropertySourcesInvokesPropertySourceProcessorInOrder() {
			BeanFactoryInitializationAotContribution contribution = getContribution(
					PropertySourceConfiguration.class, PropertySourceDependentConfiguration.class);
			contribution.applyTo(generationContext, beanFactoryInitializationCode);
			compile((initializer, compiled) -> {
				GenericApplicationContext freshContext = new GenericApplicationContext();
				ConfigurableEnvironment environment = freshContext.getEnvironment();
				assertThat(environment.containsProperty("from.p1")).isFalse();
				assertThat(environment.containsProperty("from.p2")).isFalse();
				initializer.accept(freshContext);
				assertThat(environment.containsProperty("from.p1")).isTrue();
				assertThat(environment.getProperty("from.p1")).isEqualTo("p1Value");
				assertThat(environment.containsProperty("from.p2")).isTrue();
				assertThat(environment.getProperty("from.p2")).isEqualTo("p2Value");
				freshContext.close();
			});
		}

		@Test
		void applyToWhenHasPropertySourceWithDetailsRetainsThem() {
			BeanFactoryInitializationAotContribution contribution = getContribution(
					PropertySourceWithDetailsConfiguration.class);
			contribution.applyTo(generationContext, beanFactoryInitializationCode);
			compile((initializer, compiled) -> {
				GenericApplicationContext freshContext = new GenericApplicationContext();
				ConfigurableEnvironment environment = freshContext.getEnvironment();
				assertThat(environment.getPropertySources().get("testp1")).isNull();
				initializer.accept(freshContext);
				assertThat(environment.getPropertySources().get("testp1")).isNotNull();
				freshContext.close();
			});
		}

		@SuppressWarnings("unchecked")
		private void compile(BiConsumer<Consumer<GenericApplicationContext>, Compiled> result) {
			MethodReference methodReference = beanFactoryInitializationCode.getInitializers().get(0);
			beanFactoryInitializationCode.getTypeBuilder().set(type -> {
				ArgumentCodeGenerator argCodeGenerator = ArgumentCodeGenerator
						.of(ConfigurableEnvironment.class, "applicationContext.getEnvironment()")
						.and(ArgumentCodeGenerator.of(ResourceLoader.class, "applicationContext"));
				CodeBlock methodInvocation = methodReference.toInvokeCodeBlock(argCodeGenerator,
						beanFactoryInitializationCode.getClassName());
				type.addModifiers(Modifier.PUBLIC);
				type.addSuperinterface(ParameterizedTypeName.get(Consumer.class, GenericApplicationContext.class));
				type.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
						.addParameter(GenericApplicationContext.class, "applicationContext")
						.addStatement(methodInvocation)
						.build());
			});
			generationContext.writeGeneratedContent();
			TestCompiler.forSystem().withFiles(generationContext.getGeneratedFiles()).compile(compiled ->
					result.accept(compiled.getInstance(Consumer.class), compiled));
		}


		@Configuration(proxyBeanMethods = false)
		@PropertySource("classpath:org/springframework/context/annotation/p1.properties")
		static class PropertySourceConfiguration {

		}

		@Configuration(proxyBeanMethods = false)
		@PropertySource("classpath:${base.package}/p2.properties")
		static class PropertySourceDependentConfiguration {

		}

		@Configuration(proxyBeanMethods = false)
		@PropertySource(name = "testp1", value = "classpath:org/springframework/context/annotation/p1.properties",
				ignoreResourceNotFound = true)
		static class PropertySourceWithDetailsConfiguration {

		}

	}

	@Nullable
	private BeanFactoryInitializationAotContribution getContribution(Class<?>... types) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		for (Class<?> type : types) {
			beanFactory.registerBeanDefinition(type.getName(), new RootBeanDefinition(type));
		}
		ConfigurationClassPostProcessor postProcessor = new ConfigurationClassPostProcessor();
		postProcessor.postProcessBeanFactory(beanFactory);
		return postProcessor.processAheadOfTime(beanFactory);
	}

	private void assertPostProcessorEntry(BeanPostProcessor postProcessor, Class<?> key, Class<?> value) {
		assertThat(postProcessor).extracting("importsMapping")
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(entry(key.getName(), value.getName()));
	}

}
