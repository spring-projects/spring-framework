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

package org.springframework.context.generator;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.aot.test.generator.file.SourceFile;
import org.springframework.aot.test.generator.file.SourceFiles;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generator.AotContributingBeanFactoryPostProcessor;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.generator.BeanFactoryContribution;
import org.springframework.beans.factory.generator.BeanFactoryInitialization;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.testfixture.context.generator.SimpleComponent;
import org.springframework.context.testfixture.context.generator.annotation.AutowiredComponent;
import org.springframework.context.testfixture.context.generator.annotation.InitDestroyComponent;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ApplicationContextAotGenerator}.
 *
 * @author Stephane Nicoll
 */
class ApplicationContextAotGeneratorTests {

	private static final ClassName MAIN_GENERATED_TYPE = ClassName.get("com.example", "Test");

	@Test
	void generateApplicationContextWithSimpleBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("test", new RootBeanDefinition(SimpleComponent.class));
		compile(context, toFreshApplicationContext(GenericApplicationContext::new, aotContext -> {
			assertThat(aotContext.getBeanDefinitionNames()).containsOnly("test");
			assertThat(aotContext.getBean("test")).isInstanceOf(SimpleComponent.class);
		}));
	}

	@Test
	void generateApplicationContextWithAutowiring() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME,
				BeanDefinitionBuilder.rootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		context.registerBeanDefinition("autowiredComponent", new RootBeanDefinition(AutowiredComponent.class));
		context.registerBeanDefinition("number", BeanDefinitionBuilder.rootBeanDefinition(Integer.class, "valueOf")
				.addConstructorArgValue("42").getBeanDefinition());
		compile(context, toFreshApplicationContext(GenericApplicationContext::new, aotContext -> {
			assertThat(aotContext.getBeanDefinitionNames()).containsOnly("autowiredComponent", "number");
			AutowiredComponent bean = aotContext.getBean(AutowiredComponent.class);
			assertThat(bean.getEnvironment()).isSameAs(aotContext.getEnvironment());
			assertThat(bean.getCounter()).isEqualTo(42);
		}));
	}

	@Test
	void generateApplicationContextWithInitDestroyMethods() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME,
				BeanDefinitionBuilder.rootBeanDefinition(CommonAnnotationBeanPostProcessor.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		context.registerBeanDefinition("initDestroyComponent", new RootBeanDefinition(InitDestroyComponent.class));
		compile(context, toFreshApplicationContext(GenericApplicationContext::new, aotContext -> {
			assertThat(aotContext.getBeanDefinitionNames()).containsOnly("initDestroyComponent");
			InitDestroyComponent bean = aotContext.getBean(InitDestroyComponent.class);
			assertThat(bean.events).containsExactly("init");
			aotContext.close();
			assertThat(bean.events).containsExactly("init", "destroy");
		}));
	}

	@Test
	void generateApplicationContextWithMultipleInitDestroyMethods() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME,
				BeanDefinitionBuilder.rootBeanDefinition(CommonAnnotationBeanPostProcessor.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyComponent.class);
		beanDefinition.setInitMethodName("customInit");
		beanDefinition.setDestroyMethodName("customDestroy");
		context.registerBeanDefinition("initDestroyComponent", beanDefinition);
		compile(context, toFreshApplicationContext(GenericApplicationContext::new, aotContext -> {
			assertThat(aotContext.getBeanDefinitionNames()).containsOnly("initDestroyComponent");
			InitDestroyComponent bean = aotContext.getBean(InitDestroyComponent.class);
			assertThat(bean.events).containsExactly("customInit", "init");
			aotContext.close();
			assertThat(bean.events).containsExactly("customInit", "init", "customDestroy", "destroy");
		}));
	}

	@Test
	void generateApplicationContextWitNoContributors() {
		GeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		generator.generateApplicationContext(new GenericApplicationContext(), generationContext);
		assertThat(write(generationContext.getMainGeneratedType())).contains("""
				public class Test implements ApplicationContextInitializer<GenericApplicationContext> {
					@Override
					public void initialize(GenericApplicationContext context) {
						// infrastructure
						DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
						beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
					}
				}
				""");
	}

	@Test
	void generateApplicationContextLoadsBeanFactoryContributors() {
		GeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.setClassLoader(
				new TestSpringFactoriesClassLoader("bean-factory-contributors.factories"));
		generator.generateApplicationContext(applicationContext, generationContext);
		assertThat(write(generationContext.getMainGeneratedType())).contains("""
				public class Test implements ApplicationContextInitializer<GenericApplicationContext> {
					@Override
					public void initialize(GenericApplicationContext context) {
						// infrastructure
						DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
						beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
						// Test
					}
				}
				""");
	}

	@Test
	void generateApplicationContextApplyContributionAsIsWithNewLineAtTheEnd() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		registerAotContributingBeanDefinition(applicationContext, "bpp", code -> code.add("// Hello"));
		GeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		generator.generateApplicationContext(applicationContext, generationContext);
		assertThat(write(generationContext.getMainGeneratedType())).contains("""
				public class Test implements ApplicationContextInitializer<GenericApplicationContext> {
					@Override
					public void initialize(GenericApplicationContext context) {
						// infrastructure
						DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
						beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
						// Hello
					}
				}
				""");
	}

	@Test
	void generateApplicationContextApplyMultipleContributionAsIsWithNewLineAtTheEnd() {
		GeneratedTypeContext generationContext = createGenerationContext();
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		registerAotContributingBeanDefinition(applicationContext, "bpp", code -> code.add("// Hello"));
		registerAotContributingBeanDefinition(applicationContext, "bpp2", code -> code.add("// World"));
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		generator.generateApplicationContext(applicationContext, generationContext);
		assertThat(write(generationContext.getMainGeneratedType())).contains("""
				public class Test implements ApplicationContextInitializer<GenericApplicationContext> {
					@Override
					public void initialize(GenericApplicationContext context) {
						// infrastructure
						DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
						beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
						// Hello
						// World
					}
				}
				""");
	}

	@Test
	void generateApplicationContextExcludeAotContributingBeanFactoryPostProcessorByDefault() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("test", new RootBeanDefinition(NoOpAotContributingBeanFactoryPostProcessor.class));
		compile(context, toFreshApplicationContext(GenericApplicationContext::new, aotContext ->
				assertThat(aotContext.getBeanDefinitionNames()).isEmpty()));
	}

	@Test
	void generateApplicationContextExcludeAotContributingBeanPostProcessorByDefault() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("test", new RootBeanDefinition(NoOpAotContributingBeanPostProcessor.class));
		compile(context, toFreshApplicationContext(GenericApplicationContext::new, aotContext ->
				assertThat(aotContext.getBeanDefinitionNames()).isEmpty()));
	}

	@Test
	void generateApplicationContextInvokeExcludePredicateInOrder() {
		GeneratedTypeContext generationContext = createGenerationContext();
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();
		BiPredicate<String, BeanDefinition> excludeFilter = mockExcludeFilter();
		given(excludeFilter.test(eq("bean1"), any(BeanDefinition.class))).willReturn(Boolean.FALSE);
		given(excludeFilter.test(eq("bean2"), any(BeanDefinition.class))).willReturn(Boolean.TRUE);
		applicationContext.registerBeanDefinition("bean2", new RootBeanDefinition(SimpleComponent.class));
		applicationContext.registerBeanDefinition("bean1", new RootBeanDefinition(SimpleComponent.class));
		registerAotContributingBeanDefinition(applicationContext, "bpp", code -> {}, excludeFilter);
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		generator.generateApplicationContext(applicationContext, generationContext);
		assertThat(write(generationContext.getMainGeneratedType()))
				.doesNotContain("bean2").doesNotContain("bpp")
				.contains("BeanDefinitionRegistrar.of(\"bean1\", SimpleComponent.class)");
		verify(excludeFilter).test(eq("bean2"), any(BeanDefinition.class));
		verify(excludeFilter).test("bean1", beanFactory.getMergedBeanDefinition("bean1"));
	}


	@SuppressWarnings("unchecked")
	private BiPredicate<String, BeanDefinition> mockExcludeFilter() {
		return mock(BiPredicate.class);
	}

	@SuppressWarnings("rawtypes")
	private void compile(GenericApplicationContext applicationContext, Consumer<ApplicationContextInitializer> initializer) {
		DefaultGeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		generator.generateApplicationContext(applicationContext, generationContext);
		SourceFiles sourceFiles = SourceFiles.none();
		for (JavaFile javaFile : generationContext.toJavaFiles()) {
			sourceFiles = sourceFiles.and(SourceFile.of((javaFile::writeTo)));
		}
		TestCompiler.forSystem().withSources(sourceFiles).compile(compiled -> {
			ApplicationContextInitializer instance = compiled.getInstance(ApplicationContextInitializer.class, MAIN_GENERATED_TYPE.canonicalName());
			initializer.accept(instance);
		});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T extends GenericApplicationContext> Consumer<ApplicationContextInitializer> toFreshApplicationContext(
			Supplier<T> applicationContextFactory, Consumer<T> context) {
		return applicationContextInitializer -> {
			T applicationContext = applicationContextFactory.get();
			applicationContextInitializer.initialize(applicationContext);
			applicationContext.refresh();
			context.accept(applicationContext);
		};
	}

	private DefaultGeneratedTypeContext createGenerationContext() {
		return new DefaultGeneratedTypeContext(MAIN_GENERATED_TYPE.packageName(), packageName ->
				GeneratedType.of(ClassName.get(packageName, MAIN_GENERATED_TYPE.simpleName())));
	}

	private String write(GeneratedType generatedType) {
		try {
			StringWriter out = new StringWriter();
			generatedType.toJavaFile().writeTo(out);
			return out.toString();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write " + generatedType, ex);
		}
	}

	private void registerAotContributingBeanDefinition(GenericApplicationContext context, String name,
			Consumer<CodeBlock.Builder> code) {
		registerAotContributingBeanDefinition(context, name, code,
				(beanName, beanDefinition) -> name.equals(beanName));
	}

	private void registerAotContributingBeanDefinition(GenericApplicationContext context, String name,
			Consumer<CodeBlock.Builder> code, BiPredicate<String, BeanDefinition> excludeFilter) {
		BeanFactoryContribution contribution = new TestBeanFactoryContribution(
				initialization -> initialization.contribute(code), excludeFilter);
		context.registerBeanDefinition(name, BeanDefinitionBuilder.rootBeanDefinition(
				TestAotContributingBeanFactoryPostProcessor.class, () ->
						new TestAotContributingBeanFactoryPostProcessor(contribution)).getBeanDefinition());
	}


	static class TestAotContributingBeanFactoryPostProcessor implements AotContributingBeanFactoryPostProcessor {

		@Nullable
		private final BeanFactoryContribution beanFactoryContribution;

		TestAotContributingBeanFactoryPostProcessor(@Nullable BeanFactoryContribution beanFactoryContribution) {
			this.beanFactoryContribution = beanFactoryContribution;
		}

		TestAotContributingBeanFactoryPostProcessor() {
			this(null);
		}

		@Override
		public BeanFactoryContribution contribute(ConfigurableListableBeanFactory beanFactory) {
			return this.beanFactoryContribution;
		}

	}

	static class TextAotContributingBeanFactoryPostProcessor implements AotContributingBeanFactoryPostProcessor {

		@Override
		public BeanFactoryContribution contribute(ConfigurableListableBeanFactory beanFactory) {
			return initialization -> initialization.contribute(code -> code.add("// Test\n"));
		}

	}

	static class NoOpAotContributingBeanFactoryPostProcessor implements AotContributingBeanFactoryPostProcessor {

		@Override
		public BeanFactoryContribution contribute(ConfigurableListableBeanFactory beanFactory) {
			return null;
		}
	}

	static class NoOpAotContributingBeanPostProcessor implements AotContributingBeanPostProcessor {

		@Override
		public BeanInstantiationContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
			return null;
		}

		@Override
		public int getOrder() {
			return 0;
		}

	}

	static class TestBeanFactoryContribution implements BeanFactoryContribution {

		private final Consumer<BeanFactoryInitialization> contribution;

		private final BiPredicate<String, BeanDefinition> excludeFilter;

		public TestBeanFactoryContribution(Consumer<BeanFactoryInitialization> contribution,
				BiPredicate<String, BeanDefinition> excludeFilter) {
			this.contribution = contribution;
			this.excludeFilter = excludeFilter;
		}

		@Override
		public void applyTo(BeanFactoryInitialization initialization) {
			this.contribution.accept(initialization);
		}

		@Override
		public BiPredicate<String, BeanDefinition> getBeanDefinitionExcludeFilter() {
			return this.excludeFilter;
		}

	}

	static class TestSpringFactoriesClassLoader extends ClassLoader {

		private final String factoriesName;

		TestSpringFactoriesClassLoader(String factoriesName) {
			super(RuntimeHintsPostProcessorTests.class.getClassLoader());
			this.factoriesName = factoriesName;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			if ("META-INF/spring.factories".equals(name)) {
				return super.getResources("org/springframework/context/generator/aot/" + this.factoriesName);
			}
			return super.getResources(name);
		}

	}

}
