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

import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.agent.EnabledIfRuntimeHintsAgent;
import org.springframework.aot.test.agent.RuntimeHintsInvocations;
import org.springframework.aot.test.agent.RuntimeHintsRecorder;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.testfixture.context.generator.SimpleComponent;
import org.springframework.context.testfixture.context.generator.annotation.AutowiredComponent;
import org.springframework.context.testfixture.context.generator.annotation.InitDestroyComponent;
import org.springframework.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link RuntimeHints} generation in {@link ApplicationContextAotGenerator}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
@EnabledIfRuntimeHintsAgent
class ApplicationContextAotGeneratorRuntimeHintsTests {

	@Test
	void generateApplicationContextWithSimpleBean() {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBeanDefinition("test", new RootBeanDefinition(SimpleComponent.class));
		compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
	}

	@Test
	void generateApplicationContextWithAutowiring() {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBeanDefinition("autowiredComponent", new RootBeanDefinition(AutowiredComponent.class));
		context.registerBeanDefinition("number", BeanDefinitionBuilder.rootBeanDefinition(Integer.class, "valueOf")
				.addConstructorArgValue("42").getBeanDefinition());
		compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
	}

	@Test
	void generateApplicationContextWithInitDestroyMethods() {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBeanDefinition("initDestroyComponent", new RootBeanDefinition(InitDestroyComponent.class));
		compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
	}

	@Test
	void generateApplicationContextWithMultipleInitDestroyMethods() {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyComponent.class);
		beanDefinition.setInitMethodName("customInit");
		beanDefinition.setDestroyMethodName("customDestroy");
		context.registerBeanDefinition("initDestroyComponent", beanDefinition);
		compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void compile(GenericApplicationContext applicationContext, BiConsumer<RuntimeHints, RuntimeHintsInvocations> initializationResult) {
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		TestGenerationContext generationContext = new TestGenerationContext();
		generator.processAheadOfTime(applicationContext, generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(generationContext.getGeneratedFiles()).compile(compiled -> {
			ApplicationContextInitializer instance = compiled.getInstance(ApplicationContextInitializer.class);
			GenericApplicationContext freshContext = new GenericApplicationContext();
			RuntimeHintsInvocations recordedInvocations = RuntimeHintsRecorder.record(() -> {
				instance.initialize(freshContext);
				freshContext.refresh();
				freshContext.close();
			});
			initializationResult.accept(generationContext.getRuntimeHints(), recordedInvocations);
		});
	}

}
