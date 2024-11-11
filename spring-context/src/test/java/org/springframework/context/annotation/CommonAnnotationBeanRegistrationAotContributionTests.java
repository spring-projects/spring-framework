/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.function.BiFunction;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanRegistrationCode;
import org.springframework.context.testfixture.context.annotation.PackagePrivateFieldResourceSample;
import org.springframework.context.testfixture.context.annotation.PackagePrivateMethodResourceSample;
import org.springframework.context.testfixture.context.annotation.PrivateFieldResourceSample;
import org.springframework.context.testfixture.context.annotation.PrivateMethodResourceSample;
import org.springframework.context.testfixture.context.annotation.PrivateMethodResourceWithCustomNameSample;
import org.springframework.context.testfixture.context.annotation.PublicMethodResourceSample;
import org.springframework.context.testfixture.context.annotation.subpkg.PackagePrivateFieldResourceFromParentSample;
import org.springframework.context.testfixture.context.annotation.subpkg.PackagePrivateMethodResourceFromParentSample;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AOT contributions of {@link CommonAnnotationBeanPostProcessor}.
 *
 * @author Stephane Nicoll
 */
class CommonAnnotationBeanRegistrationAotContributionTests {

	private final TestGenerationContext generationContext;

	private final MockBeanRegistrationCode beanRegistrationCode;

	private final DefaultListableBeanFactory beanFactory;

	private final CommonAnnotationBeanPostProcessor beanPostProcessor;

	CommonAnnotationBeanRegistrationAotContributionTests() {
		this.generationContext = new TestGenerationContext();
		this.beanRegistrationCode = new MockBeanRegistrationCode(this.generationContext);
		this.beanFactory = new DefaultListableBeanFactory();
		this.beanPostProcessor = new CommonAnnotationBeanPostProcessor();
		this.beanPostProcessor.setBeanFactory(this.beanFactory);
	}

	@Test
	void contributeWhenPrivateFieldInjectionInjectsUsingReflection() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = getAndApplyContribution(
				PrivateFieldResourceSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onField(PrivateFieldResourceSample.class, "one"))
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
			PrivateFieldResourceSample instance = new PrivateFieldResourceSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("one").isEqualTo("1");
			assertThat(getSourceFile(compiled, PrivateFieldResourceSample.class))
					.contains("resolveAndSet(");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void contributeWhenPackagePrivateFieldInjectionInjectsUsingFieldAssignement() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = getAndApplyContribution(
				PackagePrivateFieldResourceSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onField(PackagePrivateFieldResourceSample.class, "one"))
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
			PackagePrivateFieldResourceSample instance = new PackagePrivateFieldResourceSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("one").isEqualTo("1");
			assertThat(getSourceFile(compiled, PackagePrivateFieldResourceSample.class))
					.contains("instance.one =");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void contributeWhenPackagePrivateFieldInjectionOnParentClassInjectsUsingReflection() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = getAndApplyContribution(
				PackagePrivateFieldResourceFromParentSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onField(PackagePrivateFieldResourceSample.class, "one"))
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
			PackagePrivateFieldResourceFromParentSample instance = new PackagePrivateFieldResourceFromParentSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("one").isEqualTo("1");
			assertThat(getSourceFile(compiled, PackagePrivateFieldResourceFromParentSample.class))
					.contains("resolveAndSet");
		});
	}

	@Test
	void contributeWhenPrivateMethodInjectionInjectsUsingReflection() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = getAndApplyContribution(
				PrivateMethodResourceSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(PrivateMethodResourceSample.class, "setOne").invoke())
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
			PrivateMethodResourceSample instance = new PrivateMethodResourceSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("one").isEqualTo("1");
			assertThat(getSourceFile(compiled, PrivateMethodResourceSample.class))
					.contains("resolveAndSet(");
		});
	}

	@Test
	void contributeWhenPrivateMethodInjectionWithCustomNameInjectsUsingReflection() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = getAndApplyContribution(
				PrivateMethodResourceWithCustomNameSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(PrivateMethodResourceWithCustomNameSample.class, "setText").invoke())
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
			PrivateMethodResourceWithCustomNameSample instance = new PrivateMethodResourceWithCustomNameSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("text").isEqualTo("1");
			assertThat(getSourceFile(compiled, PrivateMethodResourceWithCustomNameSample.class))
					.contains("resolveAndSet(");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void contributeWhenPackagePrivateMethodInjectionInjectsUsingMethodInvocation() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = getAndApplyContribution(
				PackagePrivateMethodResourceSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(PackagePrivateMethodResourceSample.class, "setOne").introspect())
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
			PackagePrivateMethodResourceSample instance = new PackagePrivateMethodResourceSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("one").isEqualTo("1");
			assertThat(getSourceFile(compiled, PackagePrivateMethodResourceSample.class))
					.contains("instance.setOne(");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void contributeWhenPackagePrivateMethodInjectionOnParentClassInjectsUsingReflection() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = getAndApplyContribution(
				PackagePrivateMethodResourceFromParentSample.class);
		assertThat(RuntimeHintsPredicates.reflection()
				.onMethod(PackagePrivateMethodResourceSample.class, "setOne"))
				.accepts(this.generationContext.getRuntimeHints());
		compile(registeredBean, (postProcessor, compiled) -> {
			PackagePrivateMethodResourceFromParentSample instance = new PackagePrivateMethodResourceFromParentSample();
			postProcessor.apply(registeredBean, instance);
			assertThat(instance).extracting("one").isEqualTo("1");
			assertThat(getSourceFile(compiled, PackagePrivateMethodResourceFromParentSample.class))
					.contains("resolveAndSet(");
		});
	}

	@Test
	void contributeWhenMethodInjectionHasMatchingPropertyValue() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(PublicMethodResourceSample.class);
		beanDefinition.getPropertyValues().addPropertyValue("one", "from-property");
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		BeanRegistrationAotContribution contribution = this.beanPostProcessor
				.processAheadOfTime(RegisteredBean.of(this.beanFactory, "test"));
		assertThat(contribution).isNull();
	}

	private RegisteredBean getAndApplyContribution(Class<?> beanClass) {
		RegisteredBean registeredBean = registerBean(beanClass);
		BeanRegistrationAotContribution contribution = this.beanPostProcessor
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

	private static SourceFile getSourceFile(Compiled compiled, Class<?> sample) {
		return compiled.getSourceFileFromPackage(sample.getPackageName());
	}


	@SuppressWarnings("unchecked")
	private void compile(RegisteredBean registeredBean,
			BiConsumer<BiFunction<RegisteredBean, Object, Object>, Compiled> result) {
		Class<?> target = registeredBean.getBeanClass();
		MethodReference methodReference = this.beanRegistrationCode.getInstancePostProcessors().get(0);
		this.beanRegistrationCode.getTypeBuilder().set(type -> {
			CodeBlock methodInvocation = methodReference.toInvokeCodeBlock(
					MethodReference.ArgumentCodeGenerator.of(RegisteredBean.class, "registeredBean")
							.and(target, "instance"), this.beanRegistrationCode.getClassName());
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(ParameterizedTypeName.get(
					BiFunction.class, RegisteredBean.class, target, target));
			type.addMethod(MethodSpec.methodBuilder("apply")
					.addModifiers(Modifier.PUBLIC)
					.addParameter(RegisteredBean.class, "registeredBean")
					.addParameter(target, "instance")
					.returns(target)
					.addStatement("return $L", methodInvocation)
					.build());

		});
		this.generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(this.generationContext).printFiles(System.out)
				.compile(compiled -> result.accept(compiled.getInstance(BiFunction.class), compiled));
	}

}
