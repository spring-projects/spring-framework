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
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.aot.test.generate.compile.Compiled;
import org.springframework.aot.test.generate.compile.TestCompiler;
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


	@Test
	void processAheadOfTimeWhenNoImportAwareConfigurationReturnsNull() {
		assertThat(getContribution(SimpleConfiguration.class)).isNull();
	}

	@Test
	void applyToWhenHasImportAwareConfigurationRegistersBeanPostProcessorWithMapEntry() {
		BeanFactoryInitializationAotContribution contribution = getContribution(ImportConfiguration.class);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
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
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
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
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
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
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		assertThat(generationContext.getRuntimeHints().resources().resourcePatterns())
				.singleElement()
				.satisfies(resourceHint -> assertThat(resourceHint.getIncludes())
						.map(ResourcePatternHint::getPattern)
						.containsOnly("org/springframework/context/testfixture/context/generator/annotation/"
								+ "ImportConfiguration.class"));
	}

	@Nullable
	private BeanFactoryInitializationAotContribution getContribution(Class<?> type) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("configuration", new RootBeanDefinition(type));
		ConfigurationClassPostProcessor postProcessor = new ConfigurationClassPostProcessor();
		postProcessor.postProcessBeanFactory(beanFactory);
		return postProcessor.processAheadOfTime(beanFactory);
	}

	private void assertPostProcessorEntry(BeanPostProcessor postProcessor, Class<?> key, Class<?> value) {
		assertThat(postProcessor).extracting("importsMapping")
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(entry(key.getName(), value.getName()));
	}

	@SuppressWarnings("unchecked")
	private void compile(BiConsumer<Consumer<DefaultListableBeanFactory>, Compiled> result) {
		MethodReference methodReference = this.beanFactoryInitializationCode.getInitializers().get(0);
		this.beanFactoryInitializationCode.getTypeBuilder().set(type -> {
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(ParameterizedTypeName.get(Consumer.class, DefaultListableBeanFactory.class));
			type.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
					.addParameter(DefaultListableBeanFactory.class, "beanFactory")
					.addStatement(methodReference.toInvokeCodeBlock(CodeBlock.of("beanFactory")))
					.build());
		});
		this.generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(this.generationContext.getGeneratedFiles()).compile(compiled ->
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
