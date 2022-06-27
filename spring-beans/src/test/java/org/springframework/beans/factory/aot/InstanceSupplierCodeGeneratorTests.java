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

import java.lang.reflect.Executable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.TestBeanWithPrivateConstructor;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponent;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponent;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import org.springframework.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import org.springframework.beans.testfixture.beans.factory.generator.injection.InjectionComponent;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.testfixture.aot.generate.TestGenerationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InstanceSupplierCodeGenerator}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class InstanceSupplierCodeGeneratorTests {

	private InMemoryGeneratedFiles generatedFiles;

	private DefaultGenerationContext generationContext;

	private boolean allowDirectSupplierShortcut = false;

	private ClassName className = ClassName.get("__", "InstanceSupplierSupplier");


	@BeforeEach
	void setup() {
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new TestGenerationContext(this.generatedFiles);
	}


	@Test
	void generateWhenHasDefaultConstructor() {
		BeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			TestBean bean = getBean(beanFactory, beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(TestBean.class);
			assertThat(compiled.getSourceFile())
					.contains("InstanceSupplier.using(TestBean::new)");
		});
		assertThat(getReflectionHints().getTypeHint(TestBean.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasConstructorWithParameter() {
		BeanDefinition beanDefinition = new RootBeanDefinition(InjectionComponent.class);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("injected", "injected");
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			InjectionComponent bean = getBean(beanFactory, beanDefinition,
					instanceSupplier);
			assertThat(bean).isInstanceOf(InjectionComponent.class).extracting("bean")
					.isEqualTo("injected");
		});
		assertThat(getReflectionHints().getTypeHint(InjectionComponent.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasConstructorWithInnerClassAndDefaultConstructor() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				NoDependencyComponent.class);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			NoDependencyComponent bean = getBean(beanFactory, beanDefinition,
					instanceSupplier);
			assertThat(bean).isInstanceOf(NoDependencyComponent.class);
			assertThat(compiled.getSourceFile()).contains(
					"getBeanFactory().getBean(InnerComponentConfiguration.class).new NoDependencyComponent()");
		});
		assertThat(getReflectionHints().getTypeHint(NoDependencyComponent.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasConstructorWithInnerClassAndParameter() {
		BeanDefinition beanDefinition = new RootBeanDefinition(
				EnvironmentAwareComponent.class);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
		beanFactory.registerSingleton("environment", new StandardEnvironment());
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			EnvironmentAwareComponent bean = getBean(beanFactory, beanDefinition,
					instanceSupplier);
			assertThat(bean).isInstanceOf(EnvironmentAwareComponent.class);
			assertThat(compiled.getSourceFile()).contains(
					"getBeanFactory().getBean(InnerComponentConfiguration.class).new EnvironmentAwareComponent(");
		});
		assertThat(getReflectionHints().getTypeHint(EnvironmentAwareComponent.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasConstructorWithGeneric() {
		BeanDefinition beanDefinition = new RootBeanDefinition(
				NumberHolderFactoryBean.class);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("number", 123);
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			NumberHolder<?> bean = getBean(beanFactory, beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(NumberHolder.class);
			assertThat(bean).extracting("number").isNull(); // No property
															// actually set
			assertThat(compiled.getSourceFile()).contains("NumberHolderFactoryBean::new");
		});
		assertThat(getReflectionHints().getTypeHint(NumberHolderFactoryBean.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasPrivateConstructor() {
		BeanDefinition beanDefinition = new RootBeanDefinition(
				TestBeanWithPrivateConstructor.class);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			TestBeanWithPrivateConstructor bean = getBean(beanFactory, beanDefinition,
					instanceSupplier);
			assertThat(bean).isInstanceOf(TestBeanWithPrivateConstructor.class);
			assertThat(compiled.getSourceFile())
					.contains("resolveAndInstantiate(registeredBean)");
		});
		assertThat(getReflectionHints().getTypeHint(TestBeanWithPrivateConstructor.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INVOKE));
	}

	@Test
	void generateWhenHasFactoryMethodWithNoArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(String.class)
				.setFactoryMethodOnBean("stringBean", "config").getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			String bean = getBean(beanFactory, beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(String.class);
			assertThat(bean).isEqualTo("Hello");
			assertThat(compiled.getSourceFile()).contains(
					"getBeanFactory().getBean(SimpleConfiguration.class).stringBean()");
		});
		assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
				.satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasPrivateStaticFactoryMethodWithNoArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(String.class)
				.setFactoryMethodOnBean("privateStaticStringBean", "config")
				.getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			String bean = getBean(beanFactory, beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(String.class);
			assertThat(bean).isEqualTo("Hello");
			assertThat(compiled.getSourceFile())
					.contains("resolveAndInstantiate(registeredBean)");
		});
		assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
				.satisfies(hasMethodWithMode(ExecutableMode.INVOKE));
	}

	@Test
	void generateWhenHasStaticFactoryMethodWithNoArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(Integer.class)
				.setFactoryMethodOnBean("integerBean", "config").getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			Integer bean = getBean(beanFactory, beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(Integer.class);
			assertThat(bean).isEqualTo(42);
			assertThat(compiled.getSourceFile())
					.contains("SimpleConfiguration::integerBean");
		});
		assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
				.satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasStaticFactoryMethodWithArg() {
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(String.class)
				.setFactoryMethodOnBean("create", "config").getBeanDefinition();
		beanDefinition.setResolvedFactoryMethod(ReflectionUtils
				.findMethod(SampleFactory.class, "create", Number.class, String.class));
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SampleFactory.class).getBeanDefinition());
		beanFactory.registerSingleton("number", 42);
		beanFactory.registerSingleton("string", "test");
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			String bean = getBean(beanFactory, beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(String.class);
			assertThat(bean).isEqualTo("42test");
			assertThat(compiled.getSourceFile()).contains("SampleFactory.create(");
		});
		assertThat(getReflectionHints().getTypeHint(SampleFactory.class))
				.satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasStaticFactoryMethodCheckedException() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(Integer.class)
				.setFactoryMethodOnBean("throwingIntegerBean", "config")
				.getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		testCompiledResult(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
			Integer bean = getBean(beanFactory, beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(Integer.class);
			assertThat(bean).isEqualTo(42);
			assertThat(compiled.getSourceFile()).contains(") throws Exception {");
		});
		assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
				.satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
	}

	private ReflectionHints getReflectionHints() {
		return this.generationContext.getRuntimeHints().reflection();
	}

	private ThrowingConsumer<TypeHint> hasConstructorWithMode(ExecutableMode mode) {
		return hint -> assertThat(hint.constructors()).anySatisfy(hasMode(mode));
	}

	private ThrowingConsumer<TypeHint> hasMethodWithMode(ExecutableMode mode) {
		return hint -> assertThat(hint.methods()).anySatisfy(hasMode(mode));
	}

	private ThrowingConsumer<ExecutableHint> hasMode(ExecutableMode mode) {
		return hint -> assertThat(hint.getModes()).containsExactly(mode);
	}

	@SuppressWarnings("unchecked")
	private <T> T getBean(DefaultListableBeanFactory beanFactory,
			BeanDefinition beanDefinition, InstanceSupplier<?> instanceSupplier) {
		((RootBeanDefinition) beanDefinition).setInstanceSupplier(instanceSupplier);
		beanFactory.registerBeanDefinition("testBean", beanDefinition);
		return (T) beanFactory.getBean("testBean");
	}

	@SuppressWarnings("unchecked")
	private void testCompiledResult(DefaultListableBeanFactory beanFactory,
			BeanDefinition beanDefinition,
			BiConsumer<InstanceSupplier<?>, Compiled> result) {
		this.generationContext.writeGeneratedContent();
		DefaultListableBeanFactory registrationBeanFactory = new DefaultListableBeanFactory(
				beanFactory);
		registrationBeanFactory.registerBeanDefinition("testBean", beanDefinition);
		RegisteredBean registeredBean = RegisteredBean.of(registrationBeanFactory,
				"testBean");
		GeneratedMethods generatedMethods = new GeneratedMethods();
		InstanceSupplierCodeGenerator generator = new InstanceSupplierCodeGenerator(
				this.generationContext, this.className, generatedMethods,
				this.allowDirectSupplierShortcut);
		Executable constructorOrFactoryMethod = ConstructorOrFactoryMethodResolver
				.resolve(registeredBean);
		CodeBlock generatedCode = generator.generateCode(registeredBean,
				constructorOrFactoryMethod);
		JavaFile javaFile = createJavaFile(generatedCode, generatedMethods);
		TestCompiler.forSystem().withFiles(this.generatedFiles).compile(javaFile::writeTo,
				compiled -> result.accept(
						(InstanceSupplier<?>) compiled.getInstance(Supplier.class).get(),
						compiled));
	}

	private JavaFile createJavaFile(CodeBlock generatedCode,
			GeneratedMethods generatedMethods) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("InstanceSupplierSupplier");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(
				ParameterizedTypeName.get(Supplier.class, InstanceSupplier.class));
		builder.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC)
				.returns(InstanceSupplier.class).addStatement("return $L", generatedCode)
				.build());
		generatedMethods.doWithMethodSpecs(builder::addMethod);
		return JavaFile.builder("__", builder.build()).build();
	}

}
