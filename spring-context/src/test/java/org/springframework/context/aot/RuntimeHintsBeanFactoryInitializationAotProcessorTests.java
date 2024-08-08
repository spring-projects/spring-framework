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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ResourceBundleHint;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.aot.AotProcessingException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RuntimeHintsBeanFactoryInitializationAotProcessor}.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
class RuntimeHintsBeanFactoryInitializationAotProcessorTests {

	private GenerationContext generationContext;

	private ApplicationContextAotGenerator generator;

	@BeforeEach
	void setup() {
		this.generationContext = new TestGenerationContext();
		this.generator = new ApplicationContextAotGenerator();
	}

	@Test
	void shouldProcessRegistrarOnConfiguration() {
		GenericApplicationContext applicationContext = createApplicationContext(
				ConfigurationWithHints.class);
		this.generator.processAheadOfTime(applicationContext,
				this.generationContext);
		assertThatSampleRegistrarContributed();
	}

	@Test
	void shouldProcessRegistrarsOnInheritedConfiguration() {
		GenericApplicationContext applicationContext = createApplicationContext(
				ExtendedConfigurationWithHints.class);
		this.generator.processAheadOfTime(applicationContext,
				this.generationContext);
		assertThatInheritedSampleRegistrarContributed();
	}

	@Test
	void shouldProcessRegistrarOnBeanMethod() {
		GenericApplicationContext applicationContext = createApplicationContext(
				ConfigurationWithBeanDeclaringHints.class);
		this.generator.processAheadOfTime(applicationContext,
				this.generationContext);
		assertThatSampleRegistrarContributed();
	}

	@Test
	void shouldProcessRegistrarInSpringFactory() {
		GenericApplicationContext applicationContext = createApplicationContext();
		applicationContext.setClassLoader(
				new TestSpringFactoriesClassLoader("test-runtime-hints-aot.factories"));
		this.generator.processAheadOfTime(applicationContext,
				this.generationContext);
		assertThatSampleRegistrarContributed();
	}

	@Test
	void shouldProcessDuplicatedRegistrarsOnlyOnce() {
		GenericApplicationContext applicationContext = createApplicationContext();
		applicationContext.registerBeanDefinition("incremental1",
				new RootBeanDefinition(ConfigurationWithIncrementalHints.class));
		applicationContext.registerBeanDefinition("incremental2",
				new RootBeanDefinition(ConfigurationWithIncrementalHints.class));
		applicationContext.setClassLoader(
				new TestSpringFactoriesClassLoader("test-duplicated-runtime-hints-aot.factories"));
		IncrementalRuntimeHintsRegistrar.counter.set(0);
		this.generator.processAheadOfTime(applicationContext,
				this.generationContext);
		RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();
		assertThat(runtimeHints.resources().resourceBundleHints().map(ResourceBundleHint::getBaseName))
				.containsOnly("com.example.example0", "sample");
		assertThat(IncrementalRuntimeHintsRegistrar.counter.get()).isEqualTo(1);
	}

	@Test
	void shouldRejectRuntimeHintsRegistrarWithoutDefaultConstructor() {
		GenericApplicationContext applicationContext = createApplicationContext(
				ConfigurationWithIllegalRegistrar.class);
		assertThatExceptionOfType(AotProcessingException.class)
				.isThrownBy(() -> this.generator.processAheadOfTime(applicationContext, this.generationContext))
				.havingCause().isInstanceOf(BeanInstantiationException.class);
	}

	private void assertThatSampleRegistrarContributed() {
		Stream<ResourceBundleHint> bundleHints = this.generationContext.getRuntimeHints()
				.resources().resourceBundleHints();
		assertThat(bundleHints)
				.anyMatch(bundleHint -> "sample".equals(bundleHint.getBaseName()));
	}

	private void assertThatInheritedSampleRegistrarContributed() {
		assertThatSampleRegistrarContributed();
		Stream<ResourceBundleHint> bundleHints = this.generationContext.getRuntimeHints()
				.resources().resourceBundleHints();
		assertThat(bundleHints)
				.anyMatch(bundleHint -> "extendedSample".equals(bundleHint.getBaseName()));
	}

	private GenericApplicationContext createApplicationContext(
			Class<?>... configClasses) {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
		for (Class<?> configClass : configClasses) {
			applicationContext.registerBeanDefinition(configClass.getSimpleName(),
					new RootBeanDefinition(configClass));
		}
		return applicationContext;
	}


	@Configuration(proxyBeanMethods = false)
	@ImportRuntimeHints(SampleRuntimeHintsRegistrar.class)
	static class ConfigurationWithHints {
	}

	@Configuration(proxyBeanMethods = false)
	@ImportRuntimeHints(ExtendedSampleRuntimeHintsRegistrar.class)
	static class ExtendedConfigurationWithHints extends ConfigurationWithHints {
	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithBeanDeclaringHints {

		@Bean
		@ImportRuntimeHints(SampleRuntimeHintsRegistrar.class)
		SampleBean sampleBean() {
			return new SampleBean();
		}

	}

	public static class SampleRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerResourceBundle("sample");
		}

	}

	public static class ExtendedSampleRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerResourceBundle("extendedSample");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportRuntimeHints(IncrementalRuntimeHintsRegistrar.class)
	static class ConfigurationWithIncrementalHints {
	}

	static class IncrementalRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

		static final AtomicInteger counter = new AtomicInteger();

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.resources().registerResourceBundle("com.example.example" + counter.getAndIncrement());
		}
	}

	static class SampleBean {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportRuntimeHints(IllegalRuntimeHintsRegistrar.class)
	static class ConfigurationWithIllegalRegistrar {

	}

	public static class IllegalRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

		public IllegalRuntimeHintsRegistrar(String arg) {

		}

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerResourceBundle("sample");
		}

	}

	static class TestSpringFactoriesClassLoader extends ClassLoader {

		private final String factoriesName;

		TestSpringFactoriesClassLoader(String factoriesName) {
			super(Thread.currentThread().getContextClassLoader());
			this.factoriesName = factoriesName;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			if ("META-INF/spring/aot.factories".equals(name)) {
				return super.getResources(
						"org/springframework/context/aot/" + this.factoriesName);
			}
			return super.getResources(name);
		}

	}

}
