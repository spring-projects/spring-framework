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

package org.springframework.beans.factory.annotation;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsPredicates;
import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanRegistrationCode;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.testfixture.aot.generate.TestGenerationContext;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutowiredAnnotationBeanPostProcessor} for AOT contributions.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
class AutowiredAnnotationBeanRegistrationAotContributionTests {

	private InMemoryGeneratedFiles generatedFiles;

	private DefaultGenerationContext generationContext;

	private RuntimeHints runtimeHints;

	private MockBeanRegistrationCode beanRegistrationCode;

	private DefaultListableBeanFactory beanFactory;

	@BeforeEach
	void setup() {
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new TestGenerationContext(this.generatedFiles);
		this.runtimeHints = this.generationContext.getRuntimeHints();
		this.beanRegistrationCode = new MockBeanRegistrationCode();
		this.beanFactory = new DefaultListableBeanFactory();
	}

	@Test
	void contributeWhenPrivateFieldInjectionInjectsUsingReflection() {
		Environment environment = new StandardEnvironment();
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registeredBean = getAndApplyContribution(
				PrivateFieldInjectionSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onField(PrivateFieldInjectionSample.class, "environment").allowWrite())
				.accepts(this.runtimeHints);
		testCompiledResult(registeredBean, (postProcessor, compiled) -> {
			PrivateFieldInjectionSample instance = new PrivateFieldInjectionSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("environment").isSameAs(environment);
			assertThat(compiled.getSourceFileFromPackage(getClass().getPackageName()))
					.contains("resolveAndSet(");
		});
	}

	@Test
	@CompileWithTargetClassAccess
	void contributeWhenPackagePrivateFieldInjectionInjectsUsingConsumer() {
		Environment environment = new StandardEnvironment();
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registeredBean = getAndApplyContribution(
				PackagePrivateFieldInjectionSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onField(PackagePrivateFieldInjectionSample.class, "environment").allowWrite())
				.accepts(this.runtimeHints);
		testCompiledResult(registeredBean, (postProcessor, compiled) -> {
			PackagePrivateFieldInjectionSample instance = new PackagePrivateFieldInjectionSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("environment").isSameAs(environment);
			assertThat(compiled.getSourceFileFromPackage(getClass().getPackageName()))
					.contains("instance.environment =");
		});
	}

	@Test
	void contributeWhenPrivateMethodInjectionInjectsUsingReflection() {
		Environment environment = new StandardEnvironment();
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registeredBean = getAndApplyContribution(
				PrivateMethodInjectionSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(PrivateMethodInjectionSample.class, "setTestBean").invoke())
				.accepts(this.runtimeHints);
		testCompiledResult(registeredBean, (postProcessor, compiled) -> {
			PrivateMethodInjectionSample instance = new PrivateMethodInjectionSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("environment").isSameAs(environment);
			assertThat(compiled.getSourceFileFromPackage(getClass().getPackageName()))
					.contains("resolveAndInvoke(");
		});
	}

	@Test
	@CompileWithTargetClassAccess
	void contributeWhenPackagePrivateMethodInjectionInjectsUsingConsumer() {
		Environment environment = new StandardEnvironment();
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registeredBean = getAndApplyContribution(
				PackagePrivateMethodInjectionSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(PackagePrivateMethodInjectionSample.class, "setTestBean").introspect())
				.accepts(this.runtimeHints);
		testCompiledResult(registeredBean, (postProcessor, compiled) -> {
			PackagePrivateMethodInjectionSample instance = new PackagePrivateMethodInjectionSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("environment").isSameAs(environment);
			assertThat(compiled.getSourceFileFromPackage(getClass().getPackageName()))
					.contains("args -> instance.setTestBean(");
		});
	}

	private RegisteredBean getAndApplyContribution(Class<?> beanClass) {
		RegisteredBean registeredBean = registerBean(beanClass);
		BeanRegistrationAotContribution contribution = new AutowiredAnnotationBeanPostProcessor()
				.processAheadOfTime(registeredBean);
		assertThat(contribution).isNotNull();
		contribution.applyTo(this.generationContext, this.beanRegistrationCode);
		return registeredBean;
	}

	private RegisteredBean registerBean(Class<?> beanClass) {
		String beanName = "testBean";
		this.beanFactory.registerBeanDefinition(beanName,
				new RootBeanDefinition(beanClass));
		return RegisteredBean.of(this.beanFactory, beanName);
	}

	@SuppressWarnings("unchecked")
	private void testCompiledResult(RegisteredBean registeredBean,
			BiConsumer<BiFunction<RegisteredBean, Object, Object>, Compiled> result) {
		this.generationContext.writeGeneratedContent();
		JavaFile javaFile = createJavaFile(registeredBean.getBeanClass());
		TestCompiler.forSystem().withFiles(this.generatedFiles).compile(javaFile::writeTo,
				compiled -> result.accept(compiled.getInstance(BiFunction.class),
						compiled));
	}

	private JavaFile createJavaFile(Class<?> target) {
		MethodReference methodReference = this.beanRegistrationCode.getInstancePostProcessors()
				.get(0);
		TypeSpec.Builder builder = TypeSpec.classBuilder("TestPostProcessor");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(BiFunction.class,
				RegisteredBean.class, target, target));
		builder.addMethod(MethodSpec.methodBuilder("apply").addModifiers(Modifier.PUBLIC)
				.addParameter(RegisteredBean.class, "registeredBean")
				.addParameter(target, "instance").returns(target)
				.addStatement("return $L", methodReference.toInvokeCodeBlock(
						CodeBlock.of("registeredBean"), CodeBlock.of("instance")))
				.build());
		return JavaFile.builder("__", builder.build()).build();
	}

}
