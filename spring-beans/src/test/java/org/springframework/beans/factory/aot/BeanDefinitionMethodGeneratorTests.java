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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.aot.test.generator.file.SourceFile;
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
import org.springframework.core.ResolvableType;
import org.springframework.core.mock.MockSpringFactoriesLoader;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionMethodGenerator} and
 * {@link DefaultBeanRegistrationCodeFragments}.
 *
 * @author Phillip Webb
 */
class BeanDefinitionMethodGeneratorTests {

	private InMemoryGeneratedFiles generatedFiles;

	private DefaultGenerationContext generationContext;

	private DefaultListableBeanFactory beanFactory;

	private MockSpringFactoriesLoader springFactoriesLoader;

	private MockBeanRegistrationsCode beanRegistrationsCode;

	private BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;

	@BeforeEach
	void setup() {
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new DefaultGenerationContext(this.generatedFiles);
		this.beanFactory = new DefaultListableBeanFactory();
		this.springFactoriesLoader = new MockSpringFactoriesLoader();
		this.methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				new AotFactoriesLoader(this.beanFactory, this.springFactoriesLoader));
		this.beanRegistrationsCode = new MockBeanRegistrationsCode(
				ClassName.get("__", "Registration"));
	}

	@Test
	void generateBeanDefinitionMethodGeneratesMethod() {
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList(), Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
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
				Collections.emptyList(), Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
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
			GeneratedMethod method = beanRegistrationCode.getMethodGenerator()
					.generateMethod("postProcess")
					.using(builder -> builder.addModifiers(Modifier.STATIC)
							.addParameter(RegisteredBean.class, "registeredBean")
							.addParameter(TestBean.class, "testBean")
							.returns(TestBean.class).addCode("return new $T($S);",
									TestBean.class, "postprocessed"));
			beanRegistrationCode.addInstancePostProcessor(MethodReference.ofStatic(
					beanRegistrationCode.getClassName(), method.getName().toString()));
		};
		List<BeanRegistrationAotContribution> aotContributions = Collections
				.singletonList(aotContribution);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null, aotContributions,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
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
	void generateBeanDefinitionMethodWhenHasAttributeFilterGeneratesMethod() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		beanDefinition.setAttribute("a", "A");
		beanDefinition.setAttribute("b", "B");
		RegisteredBean registeredBean = registerBean(beanDefinition);
		List<BeanRegistrationCodeFragmentsCustomizer> fragmentCustomizers = Collections
				.singletonList(this::customizeWithAttributeFilter);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList(), fragmentCustomizers);
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
			assertThat(actual.getAttribute("a")).isEqualTo("A");
			assertThat(actual.getAttribute("b")).isNull();
		});
	}

	private BeanRegistrationCodeFragments customizeWithAttributeFilter(
			RegisteredBean registeredBean, BeanRegistrationCodeFragments codeFragments) {
		return new BeanRegistrationCodeFragments(codeFragments) {

			@Override
			public CodeBlock generateSetBeanDefinitionPropertiesCode(
					GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode,
					RootBeanDefinition beanDefinition,
					Predicate<String> attributeFilter) {
				return super.generateSetBeanDefinitionPropertiesCode(generationContext,
						beanRegistrationCode, beanDefinition, name -> "a".equals(name));
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
				Collections.emptyList(), Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
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
				Collections.emptyList(), Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
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
				Collections.emptyList(), Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
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
		aotContributions
				.add((generationContext, beanRegistrationCode) -> beanRegistrationCode
						.getMethodGenerator().generateMethod("aotContributedMethod")
						.using(builder -> builder.addComment("Example Contribution")));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null, aotContributions,
				Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("AotContributedMethod()");
			assertThat(sourceFile).contains("Example Contribution");
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasBeanRegistrationCodeFragmentsCustomizerReturnsCodeGeneratesMethod() {
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(TestBean.class));
		List<BeanRegistrationCodeFragmentsCustomizer> codeFragmentsCustomizers = new ArrayList<>();
		codeFragmentsCustomizers.add(this::customizeBeanRegistrationCodeFragments);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList(), codeFragmentsCustomizers);
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method,
				(actual, compiled) -> assertThat(
						compiled.getSourceFile(".*BeanDefinitions"))
								.contains("// Custom Code"));
	}

	private BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(
			RegisteredBean registeredBean, BeanRegistrationCodeFragments codeFragments) {
		return new BeanRegistrationCodeFragments(codeFragments) {

			@Override
			public CodeBlock generateReturnCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode) {
				CodeBlock.Builder builder = CodeBlock.builder();
				builder.addStatement("// Custom Code");
				builder.add(super.generateReturnCode(generationContext,
						beanRegistrationCode));
				return builder.build();
			}

		};
	}

	@Test
	@CompileWithTargetClassAccess(classes = PackagePrivateTestBean.class)
	void generateBeanDefinitionMethodWhenPackagePrivateBean() {
		RegisteredBean registeredBean = registerBean(
				new RootBeanDefinition(PackagePrivateTestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
				this.methodGeneratorFactory, registeredBean, null,
				Collections.emptyList(), Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(
				this.generationContext, this.beanRegistrationsCode);
		testCompiledResult(method, false, (actual, compiled) -> {
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
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, beanName);
		return registeredBean;
	}

	@SuppressWarnings("unchecked")
	private void testCompiledResult(MethodReference method,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		testCompiledResult(method, false, result);
	}

	@SuppressWarnings("unchecked")
	private void testCompiledResult(MethodReference method, boolean targetClassAccess,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		this.generationContext.writeGeneratedContent();
		JavaFile javaFile = generateJavaFile(method);
		TestCompiler.forSystem().withFiles(this.generatedFiles).printFiles(System.out)
				.compile(javaFile::writeTo, compiled -> result.accept(
						(RootBeanDefinition) compiled.getInstance(Supplier.class).get(),
						compiled));
	}

	private JavaFile generateJavaFile(MethodReference method) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("Registration");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(
				ParameterizedTypeName.get(Supplier.class, BeanDefinition.class));
		builder.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC)
				.returns(BeanDefinition.class)
				.addCode("return $L;", method.toInvokeCodeBlock()).build());
		this.beanRegistrationsCode.getGeneratedMethods()
				.doWithMethodSpecs(builder::addMethod);
		return JavaFile.builder("__", builder.build()).build();
	}

}
