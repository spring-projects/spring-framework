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
import java.net.URL;
import java.util.Enumeration;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.hint.ResourceBundleHint;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RuntimeHintsPostProcessor}.
 *
 * @author Brian Clozel
 */
class RuntimeHintsPostProcessorTests {

	private DefaultGeneratedTypeContext generationContext;

	private ApplicationContextAotGenerator generator;

	@BeforeEach
	void setup() {
		this.generationContext = createGenerationContext();
		this.generator = new ApplicationContextAotGenerator();
	}

	@Test
	void shouldProcessRegistrarOnConfiguration() {
		GenericApplicationContext applicationContext = createContext(ConfigurationWithHints.class);
		this.generator.generateApplicationContext(applicationContext, this.generationContext);
		assertThatSampleRegistrarContributed();
	}

	@Test
	void shouldProcessRegistrarOnBeanMethod() {
		GenericApplicationContext applicationContext = createContext(ConfigurationWithBeanDeclaringHints.class);
		this.generator.generateApplicationContext(applicationContext, this.generationContext);
		assertThatSampleRegistrarContributed();
	}

	@Test
	void shouldProcessRegistrarInSpringFactory() {
		GenericApplicationContext applicationContext = createContext();
		applicationContext.setClassLoader(new TestSpringFactoriesClassLoader("test.factories"));
		this.generator.generateApplicationContext(applicationContext, this.generationContext);
		assertThatSampleRegistrarContributed();
	}

	@Test
	void shouldRejectRuntimeHintsRegistrarWithoutDefaultConstructor() {
		GenericApplicationContext applicationContext = createContext(ConfigurationWithIllegalRegistrar.class);
		assertThatThrownBy(() -> this.generator.generateApplicationContext(applicationContext, this.generationContext))
				.isInstanceOf(BeanInstantiationException.class);
	}

	private void assertThatSampleRegistrarContributed() {
		Stream<ResourceBundleHint> bundleHints = this.generationContext.runtimeHints().resources().resourceBundles();
		assertThat(bundleHints).anyMatch(bundleHint -> "sample".equals(bundleHint.getBaseName()));
	}

	private GenericApplicationContext createContext(Class<?>... configClasses) {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
		for (Class<?> configClass : configClasses) {
			applicationContext.registerBeanDefinition(configClass.getSimpleName(), new RootBeanDefinition(configClass));
		}
		applicationContext.registerBeanDefinition("runtimeHintsPostProcessor",
				BeanDefinitionBuilder.rootBeanDefinition(RuntimeHintsPostProcessor.class, RuntimeHintsPostProcessor::new).getBeanDefinition());
		return applicationContext;
	}

	private DefaultGeneratedTypeContext createGenerationContext() {
		ClassName mainGeneratedType = ClassName.get("com.example", "Test");
		return new DefaultGeneratedTypeContext(mainGeneratedType.packageName(), packageName ->
				GeneratedType.of(ClassName.get(packageName, mainGeneratedType.simpleName())));
	}


	@ImportRuntimeHints(SampleRuntimeHintsRegistrar.class)
	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithHints {

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

	static class SampleBean {

	}

	@ImportRuntimeHints(IllegalRuntimeHintsRegistrar.class)
	@Configuration(proxyBeanMethods = false)
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
			super(RuntimeHintsPostProcessorTests.class.getClassLoader());
			this.factoriesName = factoriesName;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			if ("META-INF/spring.factories".equals(name)) {
				return super.getResources("org/springframework/context/generator/runtimehints/" + this.factoriesName);
			}
			return super.getResources(name);
		}

	}

}
