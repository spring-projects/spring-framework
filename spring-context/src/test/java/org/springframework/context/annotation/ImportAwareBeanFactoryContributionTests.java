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

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.beans.factory.generator.BeanFactoryContribution;
import org.springframework.beans.factory.generator.BeanFactoryInitialization;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.context.testfixture.context.generator.annotation.ImportConfiguration;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.support.CodeSnippet;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code ImportAwareBeanFactoryConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class ImportAwareBeanFactoryContributionTests {

	@Test
	void contributeWithImportAwareConfigurationRegistersBeanPostProcessor() {
		BeanFactoryContribution contribution = createContribution(ImportConfiguration.class);
		assertThat(contribution).isNotNull();
		BeanFactoryInitialization initialization = new BeanFactoryInitialization(createGenerationContext());
		contribution.applyTo(initialization);
		assertThat(CodeSnippet.of(initialization.toCodeBlock()).getSnippet()).isEqualTo("""
				beanFactory.addBeanPostProcessor(createImportAwareBeanPostProcessor());
				""");
	}

	@Test
	void contributeWithImportAwareConfigurationCreateMappingsMethod() {
		BeanFactoryContribution contribution = createContribution(ImportConfiguration.class);
		assertThat(contribution).isNotNull();
		GeneratedTypeContext generationContext = createGenerationContext();
		contribution.applyTo(new BeanFactoryInitialization(generationContext));
		assertThat(codeOf(generationContext.getMainGeneratedType())).contains("""
					private ImportAwareAotBeanPostProcessor createImportAwareBeanPostProcessor() {
						Map<String, String> mappings = new HashMap<>();
						mappings.put("org.springframework.context.testfixture.context.generator.annotation.ImportAwareConfiguration", "org.springframework.context.testfixture.context.generator.annotation.ImportConfiguration");
						return new ImportAwareAotBeanPostProcessor(mappings);
					}
				""");

	}

	@Test
	void contributeWithImportAwareConfigurationRegisterBytecodeResourceHint() {
		BeanFactoryContribution contribution = createContribution(ImportConfiguration.class);
		assertThat(contribution).isNotNull();
		GeneratedTypeContext generationContext = createGenerationContext();
		contribution.applyTo(new BeanFactoryInitialization(generationContext));
		assertThat(generationContext.runtimeHints().resources().resourcePatterns())
				.singleElement().satisfies(resourceHint -> assertThat(resourceHint.getIncludes()).containsOnly(
						"org/springframework/context/testfixture/context/generator/annotation/ImportConfiguration.class"));
	}

	@Test
	void contributeWithNoImportAwareConfigurationReturnsNull() {
		assertThat(createContribution(SimpleConfiguration.class)).isNull();
	}


	@Nullable
	private BeanFactoryContribution createContribution(Class<?> type) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("configuration", new RootBeanDefinition(type));
		ConfigurationClassPostProcessor pp = new ConfigurationClassPostProcessor();
		pp.postProcessBeanFactory(beanFactory);
		return pp.contribute(beanFactory);
	}

	private GeneratedTypeContext createGenerationContext() {
		return new DefaultGeneratedTypeContext("com.example", packageName ->
				GeneratedType.of(ClassName.get(packageName, "Test")));
	}

	private String codeOf(GeneratedType type) {
		try {
			StringWriter out = new StringWriter();
			type.toJavaFile().writeTo(out);
			return out.toString();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
