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

package org.springframework.context.aot;

import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.testfixture.context.generator.SimpleComponent;
import org.springframework.context.testfixture.context.generator.annotation.AutowiredComponent;
import org.springframework.context.testfixture.context.generator.annotation.InitDestroyComponent;
import org.springframework.core.testfixture.aot.generate.TestGenerationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationContextAotGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ApplicationContextAotGeneratorTests {

	@Test
	void generateApplicationContextWhenHasSimpleBean() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.registerBeanDefinition("test",
				new RootBeanDefinition(SimpleComponent.class));
		testCompiledResult(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
					initializer);
			assertThat(freshApplicationContext.getBeanDefinitionNames())
					.containsOnly("test");
			assertThat(freshApplicationContext.getBean("test"))
					.isInstanceOf(SimpleComponent.class);
		});
	}

	@Test
	void generateApplicationContextWhenHasAutowiring() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.registerBeanDefinition(
				AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME,
				BeanDefinitionBuilder
						.rootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		applicationContext.registerBeanDefinition("autowiredComponent",
				new RootBeanDefinition(AutowiredComponent.class));
		applicationContext.registerBeanDefinition("number",
				BeanDefinitionBuilder.rootBeanDefinition(Integer.class, "valueOf")
						.addConstructorArgValue("42").getBeanDefinition());
		testCompiledResult(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
					initializer);
			assertThat(freshApplicationContext.getBeanDefinitionNames())
					.containsOnly("autowiredComponent", "number");
			AutowiredComponent bean = freshApplicationContext
					.getBean(AutowiredComponent.class);
			assertThat(bean.getEnvironment())
					.isSameAs(freshApplicationContext.getEnvironment());
			assertThat(bean.getCounter()).isEqualTo(42);
		});
	}

	@Test
	void generateApplicationContextWhenHasInitDestroyMethods() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.registerBeanDefinition(
				AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME,
				BeanDefinitionBuilder
						.rootBeanDefinition(CommonAnnotationBeanPostProcessor.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		applicationContext.registerBeanDefinition("initDestroyComponent",
				new RootBeanDefinition(InitDestroyComponent.class));
		testCompiledResult(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
					initializer);
			assertThat(freshApplicationContext.getBeanDefinitionNames())
					.containsOnly("initDestroyComponent");
			InitDestroyComponent bean = freshApplicationContext
					.getBean(InitDestroyComponent.class);
			assertThat(bean.events).containsExactly("init");
			freshApplicationContext.close();
			assertThat(bean.events).containsExactly("init", "destroy");
		});
	}

	@Test
	void generateApplicationContextWhenHasMultipleInitDestroyMethods() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.registerBeanDefinition(
				AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME,
				BeanDefinitionBuilder
						.rootBeanDefinition(CommonAnnotationBeanPostProcessor.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				InitDestroyComponent.class);
		beanDefinition.setInitMethodName("customInit");
		beanDefinition.setDestroyMethodName("customDestroy");
		applicationContext.registerBeanDefinition("initDestroyComponent", beanDefinition);
		testCompiledResult(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
					initializer);
			assertThat(freshApplicationContext.getBeanDefinitionNames())
					.containsOnly("initDestroyComponent");
			InitDestroyComponent bean = freshApplicationContext
					.getBean(InitDestroyComponent.class);
			assertThat(bean.events).containsExactly("customInit", "init");
			freshApplicationContext.close();
			assertThat(bean.events).containsExactly("customInit", "init", "customDestroy",
					"destroy");
		});
	}

	@Test
	void generateApplicationContextWhenHasNoAotContributions() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		testCompiledResult(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
					initializer);
			assertThat(freshApplicationContext.getBeanDefinitionNames()).isEmpty();
			assertThat(compiled.getSourceFile()).contains(
					"beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver())");
		});
	}

	@Test
	void generateApplicationContextWhenHasBeanFactoryInitializationAotProcessorExcludesProcessor() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.registerBeanDefinition("test",
				new RootBeanDefinition(NoOpBeanFactoryInitializationAotProcessor.class));
		testCompiledResult(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
					initializer);
			assertThat(freshApplicationContext.getBeanDefinitionNames()).isEmpty();
		});
	}

	@Test
	void generateApplicationContextWhenHasBeanRegistrationAotProcessorExcludesProcessor() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.registerBeanDefinition("test",
				new RootBeanDefinition(NoOpBeanRegistrationAotProcessor.class));
		testCompiledResult(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
					initializer);
			assertThat(freshApplicationContext.getBeanDefinitionNames()).isEmpty();
		});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void testCompiledResult(GenericApplicationContext applicationContext,
			BiConsumer<ApplicationContextInitializer<GenericApplicationContext>, Compiled> result) {
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		DefaultGenerationContext generationContext = new TestGenerationContext(generatedFiles);
		generator.generateApplicationContext(applicationContext, generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(generatedFiles).compile(compiled ->
				result.accept(compiled.getInstance(ApplicationContextInitializer.class), compiled));
	}

	private GenericApplicationContext toFreshApplicationContext(
			ApplicationContextInitializer<GenericApplicationContext> initializer) {
		GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
		initializer.initialize(freshApplicationContext);
		freshApplicationContext.refresh();
		return freshApplicationContext;
	}


	static class NoOpBeanFactoryInitializationAotProcessor
			implements BeanFactoryPostProcessor, BeanFactoryInitializationAotProcessor {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
		}

		@Override
		public BeanFactoryInitializationAotContribution processAheadOfTime(
				ConfigurableListableBeanFactory beanFactory) {
			return null;
		}

	}


	static class NoOpBeanRegistrationAotProcessor
			implements BeanPostProcessor, BeanRegistrationAotProcessor {

		@Override
		public BeanRegistrationAotContribution processAheadOfTime(
				RegisteredBean registeredBean) {
			return null;
		}

	}

}
