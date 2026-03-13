/*
 * Copyright 2002-present the original author or authors.
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

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduction test for <a href="https://github.com/spring-projects/spring-framework/issues/35871">gh-35871</a>.
 * Prototype bean with {@code @Autowired} setter injection fails when retrieved
 * with explicit constructor args via {@code getBean(name, args)} in AOT mode.
 */
class PrototypeWithArgsAotTests {

	@Test
	void prototypeWithArgsAndAutowiredSetterInAotMode() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		registerBeanPostProcessor(applicationContext);

		// Register a singleton bean
		applicationContext.registerBeanDefinition("singletonService",
				new RootBeanDefinition(SingletonService.class));

		// Register a prototype bean with constructor arg + @Autowired setter
		RootBeanDefinition prototypeDef = new RootBeanDefinition(PrototypeService.class);
		prototypeDef.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		applicationContext.registerBeanDefinition("prototypeService", prototypeDef);

		testCompiledResult(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshContext = toFreshApplicationContext(initializer);

			// This is the key: getBean with explicit constructor args bypasses instance supplier
			PrototypeService instance = (PrototypeService) freshContext.getBean("prototypeService", "testName");

			assertThat(instance.getName()).isEqualTo("testName");
			assertThat(instance.getBeanFactory())
					.as("BeanFactoryAware should still work")
					.isNotNull();
			assertThat(instance.getSingletonService())
					.as("@Autowired setter should be called even when instance supplier is bypassed")
					.isNotNull();

			freshContext.close();
		});
	}

	private static void registerBeanPostProcessor(GenericApplicationContext applicationContext) {
		applicationContext.registerBeanDefinition(
				AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME,
				new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class));
	}

	private static TestGenerationContext processAheadOfTime(GenericApplicationContext applicationContext) {
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		TestGenerationContext generationContext = new TestGenerationContext();
		generator.processAheadOfTime(applicationContext, generationContext);
		generationContext.writeGeneratedContent();
		return generationContext;
	}

	private static void testCompiledResult(GenericApplicationContext applicationContext,
			BiConsumer<ApplicationContextInitializer<GenericApplicationContext>, Compiled> result) {
		testCompiledResult(processAheadOfTime(applicationContext), result);
	}

	@SuppressWarnings("unchecked")
	private static void testCompiledResult(TestGenerationContext generationContext,
			BiConsumer<ApplicationContextInitializer<GenericApplicationContext>, Compiled> result) {
		TestCompiler.forSystem().with(generationContext).compile(compiled ->
				result.accept(compiled.getInstance(ApplicationContextInitializer.class), compiled));
	}

	private static GenericApplicationContext toFreshApplicationContext(
			ApplicationContextInitializer<GenericApplicationContext> initializer) {
		GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
		initializer.initialize(freshApplicationContext);
		freshApplicationContext.refresh();
		return freshApplicationContext;
	}


	public static class SingletonService {
		public String getValue() {
			return "singleton";
		}
	}

	public static class PrototypeService implements BeanFactoryAware {
		private final String name;
		private SingletonService singletonService;
		private BeanFactory beanFactory;

		public PrototypeService(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public SingletonService getSingletonService() {
			return singletonService;
		}

		@Autowired
		public void setSingletonService(SingletonService singletonService) {
			this.singletonService = singletonService;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		public BeanFactory getBeanFactory() {
			return beanFactory;
		}
	}
}
