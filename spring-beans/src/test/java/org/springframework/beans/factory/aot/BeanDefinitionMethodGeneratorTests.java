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
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.AnnotatedBean;
import org.springframework.beans.testfixture.beans.GenericBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanRegistrationsCode;
import org.springframework.core.ResolvableType;
import org.springframework.core.test.io.support.MockSpringFactoriesLoader;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionMethodGenerator} and
 * {@link DefaultBeanRegistrationCodeFragments}.
 *
 * @author Phillip Webb
 */
class BeanDefinitionMethodGeneratorTests {

	private final TestGenerationContext generationContext;

	private final DefaultListableBeanFactory beanFactory;

	private final MockBeanRegistrationsCode beanRegistrationsCode;

	private final BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;


	BeanDefinitionMethodGeneratorTests() {
		this.generationContext = new TestGenerationContext();
		this.beanFactory = new DefaultListableBeanFactory();
		this.methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				AotServices.factoriesAndBeans(new MockSpringFactoriesLoader(), this.beanFactory));
		this.beanRegistrationsCode = new MockBeanRegistrationsCode(this.generationContext);
	}


	@Test
	void generateBeanDefinitionMethodGeneratesMethod() {
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
			assertThat(sourceFile).contains("beanType = TestBean.class");
			assertThat(sourceFile).contains("setInstanceSupplier(TestBean::new)");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasGenericsGeneratesMethod() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(
				ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class)));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			assertThat(actual.getResolvableType().resolve()).isEqualTo(GenericBean.class);
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
			assertThat(sourceFile).contains(
					"beanType = ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class)");
			assertThat(sourceFile).contains("setInstanceSupplier(GenericBean::new)");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasInstancePostProcessorGeneratesMethod() {
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(TestBean.class));
		BeanRegistrationAotContribution aotContribution = (generationContext,
				beanRegistrationCode) -> {
			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("postProcess", method ->
					method.addModifiers(Modifier.STATIC)
							.addParameter(RegisteredBean.class, "registeredBean")
							.addParameter(TestBean.class, "testBean")
							.returns(TestBean.class).addCode("return new $T($S);", TestBean.class, "postprocessed"));
			beanRegistrationCode.addInstancePostProcessor(generatedMethod.toMethodReference());
		};
		List<BeanRegistrationAotContribution> aotContributions = Collections
				.singletonList(aotContribution);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null, aotContributions);
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			assertThat(actual.getBeanClass()).isEqualTo(TestBean.class);
			InstanceSupplier<?> supplier = (InstanceSupplier<?>) actual
					.getInstanceSupplier();
			try {
				TestBean instance = (TestBean) supplier.get(registeredBean);
				assertThat(instance.getName()).isEqualTo("postprocessed");
			}
			catch (Exception ex) {
			}
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("instanceSupplier.andThen(");
		});
	}

	@Test // gh-28748
	void generateBeanDefinitionMethodWhenHasInstancePostProcessorAndFactoryMethodGeneratesMethod() {
		this.beanFactory.registerBeanDefinition("testBeanConfiguration", new RootBeanDefinition(TestBeanConfiguration.class));
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		beanDefinition.setFactoryBeanName("testBeanConfiguration");
		beanDefinition.setFactoryMethodName("testBean");
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanRegistrationAotContribution aotContribution = (generationContext,
				beanRegistrationCode) -> {
			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("postProcess", method ->
					method.addModifiers(Modifier.STATIC)
							.addParameter(RegisteredBean.class, "registeredBean")
							.addParameter(TestBean.class, "testBean")
							.returns(TestBean.class).addCode("return new $T($S);", TestBean.class, "postprocessed"));
			beanRegistrationCode.addInstancePostProcessor(generatedMethod.toMethodReference());
		};
		List<BeanRegistrationAotContribution> aotContributions = Collections
				.singletonList(aotContribution);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null, aotContributions);
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			assertThat(compiled.getSourceFile(".*BeanDefinitions")).contains("BeanInstanceSupplier");
			assertThat(actual.getBeanClass()).isEqualTo(TestBean.class);
			InstanceSupplier<?> supplier = (InstanceSupplier<?>) actual
					.getInstanceSupplier();
			try {
				TestBean instance = (TestBean) supplier.get(registeredBean);
				assertThat(instance.getName()).isEqualTo("postprocessed");
			}
			catch (Exception ex) {
			}
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("instanceSupplier.andThen(");
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasCodeFragmentsCustomizerGeneratesMethod() {
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(TestBean.class));
		BeanRegistrationAotContribution aotContribution = BeanRegistrationAotContribution
				.ofBeanRegistrationCodeFragmentsCustomizer(this::customizeBeanDefinitionCode);
		List<BeanRegistrationAotContribution> aotContributions = Collections.singletonList(aotContribution);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null, aotContributions);
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			assertThat(actual.getBeanClass()).isEqualTo(TestBean.class);
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("I am custom");
		});
	}

	private BeanRegistrationCodeFragments customizeBeanDefinitionCode(
			BeanRegistrationCodeFragments codeFragments) {
		return new BeanRegistrationCodeFragments(codeFragments) {

			@Override
			public CodeBlock generateNewBeanDefinitionCode(
					GenerationContext generationContext,
					ResolvableType beanType,
					BeanRegistrationCode beanRegistrationCode) {
				CodeBlock.Builder code = CodeBlock.builder();
				code.addStatement("// I am custom");
				code.add(super.generateNewBeanDefinitionCode(generationContext, beanType, beanRegistrationCode));
				return code.build();
			}

		};
	}

	@Test
	void generateBeanDefinitionMethodDoesNotGenerateAttributesByDefault() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		beanDefinition.setAttribute("a", "A");
		beanDefinition.setAttribute("b", "B");
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			assertThat(actual.hasAttribute("a")).isFalse();
			assertThat(actual.hasAttribute("b")).isFalse();
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasAttributeFilterGeneratesMethod() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		beanDefinition.setAttribute("a", "A");
		beanDefinition.setAttribute("b", "B");
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanRegistrationAotContribution aotContribution = BeanRegistrationAotContribution
				.ofBeanRegistrationCodeFragmentsCustomizer(this::customizeAttributeFilter);
		List<BeanRegistrationAotContribution> aotContributions = Collections
				.singletonList(aotContribution);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				aotContributions);
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			assertThat(actual.getAttribute("a")).isEqualTo("A");
			assertThat(actual.getAttribute("b")).isNull();
		});
	}

	private BeanRegistrationCodeFragments customizeAttributeFilter(
			BeanRegistrationCodeFragments codeFragments) {
		return new BeanRegistrationCodeFragments(codeFragments) {

			@Override
			public CodeBlock generateSetBeanDefinitionPropertiesCode(
					GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode,
					RootBeanDefinition beanDefinition,
					Predicate<String> attributeFilter) {
				return super.generateSetBeanDefinitionPropertiesCode(generationContext,
						beanRegistrationCode, beanDefinition, "a"::equals);
			}

		};
	}

	@Test
	void generateBeanDefinitionMethodWhenInnerBeanGeneratesMethod() {
		RegisteredBean parent = registerBean(new RootBeanDefinition(TestBean.class));
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(parent,
				new RootBeanDefinition(AnnotatedBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, innerBean, "testInnerBean",
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			assertThat(compiled.getSourceFile(".*BeanDefinitions"))
					.contains("Get the inner-bean definition for 'testInnerBean'");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasInnerBeanPropertyValueGeneratesMethod() {
		RootBeanDefinition innerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(AnnotatedBean.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).setPrimary(true)
				.getBeanDefinition();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		beanDefinition.getPropertyValues().add("name", innerBeanDefinition);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			RootBeanDefinition actualInnerBeanDefinition = (RootBeanDefinition) actual
					.getPropertyValues().get("name");
			assertThat(actualInnerBeanDefinition.isPrimary()).isTrue();
			assertThat(actualInnerBeanDefinition.getRole())
					.isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
			Supplier<?> innerInstanceSupplier = actualInnerBeanDefinition
					.getInstanceSupplier();
			try {
				assertThat(innerInstanceSupplier.get()).isInstanceOf(AnnotatedBean.class);
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasInnerBeanConstructorValueGeneratesMethod() {
		RootBeanDefinition innerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(String.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).setPrimary(true)
				.getBeanDefinition();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		ValueHolder valueHolder = new ValueHolder(innerBeanDefinition);
		valueHolder.setName("second");
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				valueHolder);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			RootBeanDefinition actualInnerBeanDefinition = (RootBeanDefinition) actual
					.getConstructorArgumentValues()
					.getIndexedArgumentValue(0, RootBeanDefinition.class).getValue();
			assertThat(actualInnerBeanDefinition.isPrimary()).isTrue();
			assertThat(actualInnerBeanDefinition.getRole())
					.isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
			Supplier<?> innerInstanceSupplier = actualInnerBeanDefinition
					.getInstanceSupplier();
			try {
				assertThat(innerInstanceSupplier.get()).isInstanceOf(String.class);
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
			assertThat(compiled.getSourceFile(".*BeanDefinitions"))
					.contains("getSecondBeanDefinition()");
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasAotContributionsAppliesContributions() {
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(TestBean.class));
		List<BeanRegistrationAotContribution> aotContributions = new ArrayList<>();
		aotContributions.add((generationContext, beanRegistrationCode) ->
				beanRegistrationCode.getMethods().add("aotContributedMethod", method ->
						method.addComment("Example Contribution")));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null, aotContributions);
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("AotContributedMethod()");
			assertThat(sourceFile).contains("Example Contribution");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void generateBeanDefinitionMethodWhenPackagePrivateBean() {
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(PackagePrivateTestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			freshBeanFactory.registerBeanDefinition("test", actual);
			Object bean = freshBeanFactory.getBean("test");
			assertThat(bean).isInstanceOf(PackagePrivateTestBean.class);
			assertThat(compiled.getSourceFileFromPackage(
					PackagePrivateTestBean.class.getPackageName())).isNotNull();
		});
	}

	private RegisteredBean registerBean(RootBeanDefinition beanDefinition) {
		String beanName = "testBean";
		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
		return RegisteredBean.of(this.beanFactory, beanName);
	}

	private void compile(MethodReference method,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		this.beanRegistrationsCode.getTypeBuilder().set(type -> {
			CodeBlock methodInvocation = method.toInvokeCodeBlock(ArgumentCodeGenerator.none(),
					this.beanRegistrationsCode.getClassName());
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(ParameterizedTypeName.get(Supplier.class, BeanDefinition.class));
			type.addMethod(MethodSpec.methodBuilder("get")
					.addModifiers(Modifier.PUBLIC)
					.returns(BeanDefinition.class)
					.addCode("return $L;", methodInvocation).build());
		});
		this.generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(this.generationContext.getGeneratedFiles()).compile(compiled ->
				result.accept((RootBeanDefinition) compiled.getInstance(Supplier.class).get(), compiled));
	}

}
