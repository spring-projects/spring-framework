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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.context.testfixture.context.generator.annotation.ImportAwareConfiguration;
import org.springframework.context.testfixture.context.generator.annotation.ImportConfiguration;
import org.springframework.core.testfixture.aot.generate.TestGenerationContext;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConfigurationClassPostProcessor} AOT contributions.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ConfigurationClassPostProcessorAotContributionTests {

	private final InMemoryGeneratedFiles generatedFiles;

	private final DefaultGenerationContext generationContext;

	private final MockBeanFactoryInitializationCode beanFactoryInitializationCode;


	ConfigurationClassPostProcessorAotContributionTests() {
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new TestGenerationContext(this.generatedFiles);
		this.beanFactoryInitializationCode = new MockBeanFactoryInitializationCode(this.generationContext);
	}


	@Test
	void applyToWhenHasImportAwareConfigurationRegistersBeanPostProcessorWithMapEntry() {
		BeanFactoryInitializationAotContribution contribution = getContribution(
				ImportConfiguration.class);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		compile((initializer, compiled) -> {
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			initializer.accept(freshBeanFactory);
			ImportAwareAotBeanPostProcessor postProcessor = (ImportAwareAotBeanPostProcessor) freshBeanFactory
					.getBeanPostProcessors().get(0);
			assertPostProcessorEntry(postProcessor, ImportAwareConfiguration.class,
					ImportConfiguration.class);
		});
	}

	@Test
	void applyToWhenHasImportAwareConfigurationRegistersHints() {
		BeanFactoryInitializationAotContribution contribution = getContribution(
				ImportConfiguration.class);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		assertThat(generationContext.getRuntimeHints().resources().resourcePatterns())
				.singleElement()
				.satisfies(resourceHint -> assertThat(resourceHint.getIncludes())
						.map(ResourcePatternHint::getPattern)
						.containsOnly("org/springframework/context/testfixture/context/generator/annotation/"
								+ "ImportConfiguration.class"));
	}

	@Test
	void processAheadOfTimeWhenNoImportAwareConfigurationReturnsNull() {
		assertThat(getContribution(SimpleConfiguration.class)).isNull();
	}

	@Nullable
	private BeanFactoryInitializationAotContribution getContribution(Class<?> type) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("configuration", new RootBeanDefinition(type));
		ConfigurationClassPostProcessor postProcessor = new ConfigurationClassPostProcessor();
		postProcessor.postProcessBeanFactory(beanFactory);
		return postProcessor.processAheadOfTime(beanFactory);
	}

	@SuppressWarnings("unchecked")
	private void compile(BiConsumer<Consumer<DefaultListableBeanFactory>, Compiled> result) {
		MethodReference methodReference = this.beanFactoryInitializationCode
				.getInitializers().get(0);
		this.beanFactoryInitializationCode.getTypeBuilder().set(type -> {
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(ParameterizedTypeName.get(Consumer.class, DefaultListableBeanFactory.class));
			type.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
					.addParameter(DefaultListableBeanFactory.class, "beanFactory")
					.addStatement(methodReference.toInvokeCodeBlock(CodeBlock.of("beanFactory")))
					.build());
		});
		this.generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(this.generatedFiles).compile(compiled ->
				result.accept(compiled.getInstance(Consumer.class), compiled));
	}

	private void assertPostProcessorEntry(ImportAwareAotBeanPostProcessor postProcessor,
			Class<?> key, Class<?> value) {
		assertThat(postProcessor).extracting("importsMapping")
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(entry(key.getName(), value.getName()));
	}

}
