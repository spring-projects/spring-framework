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

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.beans.factory.generator.BeanFactoryInitialization;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.support.CodeSnippet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfrastructureContribution}.
 *
 * @author Stephane Nicoll
 */
class InfrastructureContributionTests {

	@Test
	void contributeInfrastructure() {
		CodeSnippet codeSnippet = contribute(createGenerationContext());
		assertThat(codeSnippet.getSnippet()).isEqualTo("""
				// infrastructure
				DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
				beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
				""");
		assertThat(codeSnippet.hasImport(ContextAnnotationAutowireCandidateResolver.class)).isTrue();
	}

	private CodeSnippet contribute(GeneratedTypeContext generationContext) {
		InfrastructureContribution contribution = new InfrastructureContribution();
		BeanFactoryInitialization initialization = new BeanFactoryInitialization(generationContext);
		contribution.applyTo(initialization);
		return CodeSnippet.of(initialization.toCodeBlock());
	}

	private GeneratedTypeContext createGenerationContext() {
		return new DefaultGeneratedTypeContext("com.example", packageName ->
				GeneratedType.of(ClassName.get(packageName, "Test")));
	}

}
