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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.AnnotatedBean;
import org.springframework.beans.testfixture.beans.GenericBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.aot.InnerBeanConfiguration;
import org.springframework.beans.testfixture.beans.factory.aot.MockBeanRegistrationsCode;
import org.springframework.beans.testfixture.beans.factory.aot.SimpleBean;
import org.springframework.beans.testfixture.beans.factory.aot.TestHierarchy;
import org.springframework.beans.testfixture.beans.factory.aot.TestHierarchy.Implementation;
import org.springframework.beans.testfixture.beans.factory.aot.TestHierarchy.One;
import org.springframework.beans.testfixture.beans.factory.aot.TestHierarchy.Two;
import org.springframework.core.ResolvableType;
import org.springframework.core.test.io.support.MockSpringFactoriesLoader;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BeanDefinitionMethodGenerator} and
 * {@link DefaultBeanRegistrationCodeFragments}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
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
	void generateBeanDefinitionMethodWithOnlyTargetTypeDoesNotSetBeanClass() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(TestBean.class);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
			assertThat(sourceFile).contains("new RootBeanDefinition()");
			assertThat(sourceFile).contains("setTargetType(TestBean.class)");
			assertThat(sourceFile).contains("setInstanceSupplier(TestBean::new)");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodSpecifiesBeanClassIfSet() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
			assertThat(sourceFile).contains("new RootBeanDefinition(TestBean.class)");
			assertThat(sourceFile).doesNotContain("setTargetType(");
			assertThat(sourceFile).contains("setInstanceSupplier(TestBean::new)");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodSpecifiesBeanClassAndTargetTypIfDifferent() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(One.class);
		beanDefinition.setTargetType(Implementation.class);
		beanDefinition.setResolvedFactoryMethod(ReflectionUtils.findMethod(TestHierarchy.class, "oneBean"));
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
			assertThat(sourceFile).contains("new RootBeanDefinition(TestHierarchy.One.class)");
			assertThat(sourceFile).contains("setTargetType(TestHierarchy.Implementation.class)");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodUSeBeanClassNameIfNotReachable() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(PackagePrivateTestBean.class);
		beanDefinition.setTargetType(TestBean.class);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
			assertThat(sourceFile).contains("new RootBeanDefinition(\"org.springframework.beans.factory.aot.PackagePrivateTestBean\"");
			assertThat(sourceFile).contains("setTargetType(TestBean.class)");
			assertThat(sourceFile).contains("setInstanceSupplier(TestBean::new)");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test // gh-29556
	void generateBeanDefinitionMethodGeneratesMethodWithInstanceSupplier() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class, TestBean::new));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				List.of((generationContext, beanRegistrationCode) -> { }));
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
			assertThat(sourceFile).contains("setInstanceSupplier(TestBean::new)");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasInnerClassTargetMethodGeneratesMethod() {
		this.beanFactory.registerBeanDefinition("testBeanConfiguration", new RootBeanDefinition(
				InnerBeanConfiguration.Simple.class));
		RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleBean.class);
		beanDefinition.setFactoryBeanName("testBeanConfiguration");
		beanDefinition.setFactoryMethodName("simpleBean");
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile.getClassName()).endsWith("InnerBeanConfiguration__BeanDefinitions");
			assertThat(sourceFile).contains("public static class Simple")
					.contains("Bean definitions for {@link InnerBeanConfiguration.Simple}")
					.doesNotContain("Another__BeanDefinitions");

		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasNestedInnerClassTargetMethodGeneratesMethod() {
		this.beanFactory.registerBeanDefinition("testBeanConfiguration", new RootBeanDefinition(
				InnerBeanConfiguration.Simple.Another.class));
		RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleBean.class);
		beanDefinition.setFactoryBeanName("testBeanConfiguration");
		beanDefinition.setFactoryMethodName("anotherBean");
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile.getClassName()).endsWith("InnerBeanConfiguration__BeanDefinitions");
			assertThat(sourceFile).contains("public static class Simple")
					.contains("Bean definitions for {@link InnerBeanConfiguration.Simple}")
					.contains("public static class Another")
					.contains("Bean definitions for {@link InnerBeanConfiguration.Simple.Another}");
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasGenericsGeneratesMethod() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class));
		RegisteredBean registeredBean = registerBean(beanDefinition);
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
					"setTargetType(ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class))");
			assertThat(sourceFile).contains("setInstanceSupplier(GenericBean::new)");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasExplicitResolvableType() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(One.class);
		beanDefinition.setResolvedFactoryMethod(ReflectionUtils.findMethod(TestHierarchy.class, "oneBean"));
		beanDefinition.setTargetType(Two.class);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> assertThat(actual.getResolvableType().resolve()).isEqualTo(Two.class));
	}

	@Test
	void generateBeanDefinitionMethodWhenHasInstancePostProcessorGeneratesMethod() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		BeanRegistrationAotContribution aotContribution = (generationContext, beanRegistrationCode) -> {
			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("postProcess", method ->
					method.addModifiers(Modifier.STATIC)
							.addParameter(RegisteredBean.class, "registeredBean")
							.addParameter(TestBean.class, "testBean")
							.returns(TestBean.class).addCode("return new $T($S);", TestBean.class, "postprocessed"));
			beanRegistrationCode.addInstancePostProcessor(generatedMethod.toMethodReference());
		};
		List<BeanRegistrationAotContribution> aotContributions = Collections.singletonList(aotContribution);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null, aotContributions);
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			assertThat(actual.getBeanClass()).isEqualTo(TestBean.class);
			InstanceSupplier<?> supplier = (InstanceSupplier<?>) actual.getInstanceSupplier();
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

	@Test  // gh-28748
	void generateBeanDefinitionMethodWhenHasInstancePostProcessorAndFactoryMethodGeneratesMethod() {
		this.beanFactory.registerBeanDefinition("testBeanConfiguration",
				new RootBeanDefinition(TestBeanConfiguration.class));
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
		List<BeanRegistrationAotContribution> aotContributions = Collections.singletonList(aotContribution);
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
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		BeanRegistrationAotContribution aotContribution =
				BeanRegistrationAotContribution.withCustomCodeFragments(this::customizeBeanDefinitionCode);
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

	private BeanRegistrationCodeFragments customizeBeanDefinitionCode(BeanRegistrationCodeFragments codeFragments) {
		return new BeanRegistrationCodeFragmentsDecorator(codeFragments) {
			@Override
			public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
					ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {
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
		BeanRegistrationAotContribution aotContribution =
				BeanRegistrationAotContribution.withCustomCodeFragments(this::customizeAttributeFilter);
		List<BeanRegistrationAotContribution> aotContributions = Collections.singletonList(aotContribution);
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

	private BeanRegistrationCodeFragments customizeAttributeFilter(BeanRegistrationCodeFragments codeFragments) {
		return new BeanRegistrationCodeFragmentsDecorator(codeFragments) {
			@Override
			public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
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

	@SuppressWarnings("unchecked")
	@Test
	void generateBeanDefinitionMethodWhenHasListOfInnerBeansPropertyValueGeneratesMethod() {
		RootBeanDefinition firstInnerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(TestBean.class).addPropertyValue("name", "one")
				.getBeanDefinition();
		RootBeanDefinition secondInnerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(TestBean.class).addPropertyValue("name", "two")
				.getBeanDefinition();
		ManagedList<RootBeanDefinition> list = new ManagedList<>();
		list.add(firstInnerBeanDefinition);
		list.add(secondInnerBeanDefinition);
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		beanDefinition.getPropertyValues().add("someList", list);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			ManagedList<RootBeanDefinition> actualPropertyValue = (ManagedList<RootBeanDefinition>) actual
					.getPropertyValues().get("someList");
			assertThat(actualPropertyValue).hasSize(2);
			assertThat(actualPropertyValue.get(0).getPropertyValues().get("name")).isEqualTo("one");
			assertThat(actualPropertyValue.get(1).getPropertyValues().get("name")).isEqualTo("two");
			assertThat(compiled.getSourceFileFromPackage(TestBean.class.getPackageName()))
					.contains("getSomeListBeanDefinition()", "getSomeListBeanDefinition1()");
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
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(PackagePrivateTestBean.class));
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

	@Test
	void generateBeanDefinitionMethodWhenBeanIsInJavaPackage() {
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(String.class).addConstructorArgValue("test").getBeanDefinition();
		testBeanDefinitionMethodInCurrentFile(String.class, beanDefinition);
	}

	@Test
	void generateBeanDefinitionMethodWhenBeanIsInJavaxPackage() {
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(DocumentBuilderFactory.class).setFactoryMethod("newDefaultInstance").getBeanDefinition();
		testBeanDefinitionMethodInCurrentFile(DocumentBuilderFactory.class, beanDefinition);
	}

	@Test
	void generateBeanDefinitionMethodWhenBeanIsOfPrimitiveType() {
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(Boolean.class).setFactoryMethod("parseBoolean").addConstructorArgValue("true").getBeanDefinition();
		testBeanDefinitionMethodInCurrentFile(Boolean.class, beanDefinition);
	}

	@Test // gh-29556
	void throwExceptionWithInstanceSupplierWithoutAotContribution() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class, TestBean::new));
		assertThatIllegalArgumentException().isThrownBy(() -> new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList()));
	}

	private void testBeanDefinitionMethodInCurrentFile(Class<?> targetType, RootBeanDefinition beanDefinition) {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(beanDefinition));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		compile(method, (actual, compiled) -> {
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			freshBeanFactory.registerBeanDefinition("test", actual);
			Object bean = freshBeanFactory.getBean("test");
			assertThat(bean).isInstanceOf(targetType);
			assertThat(compiled.getSourceFiles().stream().filter(sourceFile ->
					sourceFile.getClassName().startsWith(targetType.getPackageName()))).isEmpty();
		});
	}

	private RegisteredBean registerBean(RootBeanDefinition beanDefinition) {
		String beanName = "testBean";
		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
		return RegisteredBean.of(this.beanFactory, beanName);
	}

	private void compile(MethodReference method, BiConsumer<RootBeanDefinition, Compiled> result) {
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
		TestCompiler.forSystem().with(this.generationContext).compile(compiled ->
				result.accept((RootBeanDefinition) compiled.getInstance(Supplier.class).get(), compiled));
	}

}
