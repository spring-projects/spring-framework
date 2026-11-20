/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.generator;

import java.util.function.BiConsumer;

import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.agent.EnabledIfRuntimeHintsAgent;
import org.springframework.aot.test.agent.RuntimeHintsInvocations;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.testfixture.context.annotation.AutowiredComponent;
import org.springframework.context.testfixture.context.annotation.InitDestroyComponent;
import org.springframework.context.testfixture.context.generator.SimpleComponent;
import org.springframework.core.test.tools.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link RuntimeHints} generation in {@link ApplicationContextAotGenerator}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
@EnabledIfRuntimeHintsAgent
@SuppressWarnings("removal")
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
		context.registerBeanDefinition("number", BeanDefinitionBuilder.rootBeanDefinition(
				Integer.class, "valueOf").addConstructorArgValue("42").getBeanDefinition());
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

	@Test
	void generateApplicationContextWithInheritedDestroyMethods() {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(InheritedDestroy.class);
		context.registerBeanDefinition("initDestroyComponent", beanDefinition);
		compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void compile(GenericApplicationContext applicationContext,
			BiConsumer<RuntimeHints, RuntimeHintsInvocations> initializationResult) {

		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		TestGenerationContext generationContext = new TestGenerationContext();
		generator.processAheadOfTime(applicationContext, generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(generationContext).compile(compiled -> {
			ApplicationContextInitializer instance = compiled.getInstance(ApplicationContextInitializer.class);
			GenericApplicationContext freshContext = new GenericApplicationContext();
			RuntimeHintsInvocations recordedInvocations = org.springframework.aot.test.agent.RuntimeHintsRecorder.record(() -> {
				instance.initialize(freshContext);
				freshContext.refresh();
				freshContext.close();
			});
			initializationResult.accept(generationContext.getRuntimeHints(), recordedInvocations);
		});
	}


	public interface Destroyable {

		@PreDestroy
		default void destroy() {
		}
	}

	public static class InheritedDestroy implements Destroyable {
	}

}
