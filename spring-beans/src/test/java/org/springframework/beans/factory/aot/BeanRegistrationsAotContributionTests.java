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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.aot.test.generator.file.SourceFile;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.mock.MockSpringFactoriesLoader;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanRegistrationsAotContribution}.
 *
 * @author Phillip Webb
 */
class BeanRegistrationsAotContributionTests {

	private InMemoryGeneratedFiles generatedFiles;

	private DefaultGenerationContext generationContext;

	private DefaultListableBeanFactory beanFactory;

	private MockSpringFactoriesLoader springFactoriesLoader;

	private BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;

	private MockBeanFactoryInitializationCode beanFactoryInitializationCode = new MockBeanFactoryInitializationCode();

	@BeforeEach
	void setup() {
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new DefaultGenerationContext(this.generatedFiles);
		this.beanFactory = new DefaultListableBeanFactory();
		this.springFactoriesLoader = new MockSpringFactoriesLoader();
		this.methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				new AotFactoriesLoader(this.beanFactory, this.springFactoriesLoader));
	}

	@Test
	void applyToAppliesContribution() {
		Map<String, BeanDefinitionMethodGenerator> registrations = new LinkedHashMap<>();
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		registrations.put("testBean", generator);
		BeanRegistrationsAotContribution contribution = new BeanRegistrationsAotContribution(
				registrations);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		testCompiledResult((consumer, compiled) -> {
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			consumer.accept(freshBeanFactory);
			assertThat(freshBeanFactory.getBean(TestBean.class)).isNotNull();
		});
	}

	@Test
	void applyToWhenHasNameGeneratesPrefixedFeatureName() {
		this.beanFactoryInitializationCode = new MockBeanFactoryInitializationCode("Management");
		Map<String, BeanDefinitionMethodGenerator> registrations = new LinkedHashMap<>();
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		registrations.put("testBean", generator);
		BeanRegistrationsAotContribution contribution = new BeanRegistrationsAotContribution(
				registrations);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		testCompiledResult((consumer, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile.getClassName()).endsWith("__ManagementBeanDefinitions");
		});
	}

	@Test
	void applyToCallsRegistrationsWithBeanRegistrationsCode() {
		List<BeanRegistrationsCode> beanRegistrationsCodes = new ArrayList<>();
		Map<String, BeanDefinitionMethodGenerator> registrations = new LinkedHashMap<>();
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList()) {

			@Override
			MethodReference generateBeanDefinitionMethod(
					GenerationContext generationContext, String featureNamePrefix,
					BeanRegistrationsCode beanRegistrationsCode) {
				beanRegistrationsCodes.add(beanRegistrationsCode);
				return super.generateBeanDefinitionMethod(generationContext,
						featureNamePrefix, beanRegistrationsCode);
			}

		};
		registrations.put("testBean", generator);
		BeanRegistrationsAotContribution contribution = new BeanRegistrationsAotContribution(
				registrations);
		contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
		assertThat(beanRegistrationsCodes).hasSize(1);
		BeanRegistrationsCode actual = beanRegistrationsCodes.get(0);
		assertThat(actual.getMethodGenerator()).isNotNull();
	}

	private RegisteredBean registerBean(RootBeanDefinition rootBeanDefinition) {
		String beanName = "testBean";
		this.beanFactory.registerBeanDefinition(beanName, rootBeanDefinition);
		return RegisteredBean.of(this.beanFactory, beanName);
	}

	@SuppressWarnings({ "unchecked", "cast" })
	private void testCompiledResult(
			BiConsumer<Consumer<DefaultListableBeanFactory>, Compiled> result) {
		this.generationContext.writeGeneratedContent();
		JavaFile javaFile = createJavaFile();
		TestCompiler.forSystem().withFiles(this.generatedFiles).compile(javaFile::writeTo,
				compiled -> result.accept(compiled.getInstance(Consumer.class),
						compiled));
	}

	private JavaFile createJavaFile() {
		MethodReference initializer = this.beanFactoryInitializationCode.initializers
				.get(0);
		TypeSpec.Builder builder = TypeSpec.classBuilder("BeanFactoryConsumer");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(Consumer.class,
				DefaultListableBeanFactory.class));
		builder.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
				.addParameter(DefaultListableBeanFactory.class, "beanFactory")
				.addStatement(initializer.toInvokeCodeBlock(CodeBlock.of("beanFactory")))
				.build());
		return JavaFile.builder("__", builder.build()).build();
	}

	class MockBeanFactoryInitializationCode implements BeanFactoryInitializationCode {

		private final GeneratedMethods generatedMethods = new GeneratedMethods();

		private final List<MethodReference> initializers = new ArrayList<>();

		private final String name;

		MockBeanFactoryInitializationCode() {
			this("");
		}

		MockBeanFactoryInitializationCode(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public MethodGenerator getMethodGenerator() {
			return this.generatedMethods;
		}

		@Override
		public void addInitializer(MethodReference methodReference) {
			this.initializers.add(methodReference);
		}

	}

}
