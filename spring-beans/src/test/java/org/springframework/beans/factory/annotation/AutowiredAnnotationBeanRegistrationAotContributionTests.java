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

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanRegistrationCode;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutowiredAnnotationBeanPostProcessor} for AOT contributions.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
class AutowiredAnnotationBeanRegistrationAotContributionTests {

	private final TestGenerationContext generationContext;

	private final MockBeanRegistrationCode beanRegistrationCode;

	private final DefaultListableBeanFactory beanFactory;


	AutowiredAnnotationBeanRegistrationAotContributionTests() {
		this.generationContext = new TestGenerationContext();
		this.beanRegistrationCode = new MockBeanRegistrationCode(this.generationContext);
		this.beanFactory = new DefaultListableBeanFactory();
	}


	@Test
	void contributeWhenPrivateFieldInjectionInjectsUsingReflection() {
		Environment environment = new StandardEnvironment();
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registeredBean = getAndApplyContribution(
				PrivateFieldInjectionSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onField(PrivateFieldInjectionSample.class, "environment"))
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
			PrivateFieldInjectionSample instance = new PrivateFieldInjectionSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("environment").isSameAs(environment);
			assertThat(compiled.getSourceFileFromPackage(getClass().getPackageName()))
					.contains("resolveAndSet(");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void contributeWhenPackagePrivateFieldInjectionInjectsUsingConsumer() {
		Environment environment = new StandardEnvironment();
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registeredBean = getAndApplyContribution(
				PackagePrivateFieldInjectionSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onField(PackagePrivateFieldInjectionSample.class, "environment"))
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
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
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
			PrivateMethodInjectionSample instance = new PrivateMethodInjectionSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("environment").isSameAs(environment);
			assertThat(compiled.getSourceFileFromPackage(getClass().getPackageName()))
					.contains("resolveAndInvoke(");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void contributeWhenPackagePrivateMethodInjectionInjectsUsingConsumer() {
		Environment environment = new StandardEnvironment();
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registeredBean = getAndApplyContribution(
				PackagePrivateMethodInjectionSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(PackagePrivateMethodInjectionSample.class, "setTestBean").introspect())
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
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
	private void compile(RegisteredBean registeredBean,
			BiConsumer<BiFunction<RegisteredBean, Object, Object>, Compiled> result) {
		Class<?> target = registeredBean.getBeanClass();
		MethodReference methodReference = this.beanRegistrationCode.getInstancePostProcessors().get(0);
		this.beanRegistrationCode.getTypeBuilder().set(type -> {
			CodeBlock methodInvocation = methodReference.toInvokeCodeBlock(
					ArgumentCodeGenerator.of(RegisteredBean.class, "registeredBean").and(target, "instance"),
					this.beanRegistrationCode.getClassName());
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(ParameterizedTypeName.get(BiFunction.class, RegisteredBean.class, target, target));
			type.addMethod(MethodSpec.methodBuilder("apply")
					.addModifiers(Modifier.PUBLIC)
					.addParameter(RegisteredBean.class, "registeredBean")
					.addParameter(target, "instance").returns(target)
					.addStatement("return $L", methodInvocation)
					.build());

		});
		this.generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(this.generationContext.getGeneratedFiles()).compile(compiled ->
				result.accept(compiled.getInstance(BiFunction.class), compiled));
	}

}
