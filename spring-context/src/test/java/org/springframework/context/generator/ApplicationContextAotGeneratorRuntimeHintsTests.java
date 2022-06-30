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

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.agent.EnabledIfRuntimeHintsAgent;
import org.springframework.aot.test.agent.RuntimeHintsInvocations;
import org.springframework.aot.test.agent.RuntimeHintsRecorder;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.testfixture.context.generator.SimpleComponent;
import org.springframework.context.testfixture.context.generator.annotation.AutowiredComponent;
import org.springframework.context.testfixture.context.generator.annotation.InitDestroyComponent;
import org.springframework.core.testfixture.aot.generate.TestGenerationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link org.springframework.aot.hint.RuntimeHints} generation in {@link ApplicationContextAotGenerator}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
@EnabledIfRuntimeHintsAgent
class ApplicationContextAotGeneratorRuntimeHintsTests {

	@Test
	void generateApplicationContextWithSimpleBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("test", new RootBeanDefinition(SimpleComponent.class));
		compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
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
		compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
	}

	@Test
	void generateApplicationContextWithInitDestroyMethods() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME,
				BeanDefinitionBuilder.rootBeanDefinition(CommonAnnotationBeanPostProcessor.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		context.registerBeanDefinition("initDestroyComponent", new RootBeanDefinition(InitDestroyComponent.class));
		compile(context, (hints, invocations) -> assertThat(invocations).withRegistrar(new InitDestroyIssueRegistrar()).match(hints));
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
		compile(context, (hints, invocations) -> assertThat(invocations).withRegistrar(new InitDestroyIssueRegistrar()).match(hints));
	}

	// TODO: Remove once https://github.com/spring-projects/spring-framework/issues/28215 is fixed
	static class InitDestroyIssueRegistrar implements RuntimeHintsRegistrar {
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.reflection()
					.registerType(TypeReference.of(InitDestroyComponent.class.getName()), typeHint ->
							typeHint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void compile(GenericApplicationContext applicationContext, BiConsumer<RuntimeHints, RuntimeHintsInvocations> initializationResult) {
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		DefaultGenerationContext generationContext = new TestGenerationContext(generatedFiles);
		generator.generateApplicationContext(applicationContext, generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(generatedFiles).compile(compiled -> {
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
