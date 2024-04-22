/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RegisteredBean.InstantiationDescriptor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.TestBeanWithPrivateConstructor;
import org.springframework.beans.testfixture.beans.factory.aot.DefaultSimpleBeanContract;
import org.springframework.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import org.springframework.beans.testfixture.beans.factory.aot.SimpleBean;
import org.springframework.beans.testfixture.beans.factory.aot.SimpleBeanContract;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponent;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponent;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.deprecation.DeprecatedBean;
import org.springframework.beans.testfixture.beans.factory.generator.deprecation.DeprecatedConstructor;
import org.springframework.beans.testfixture.beans.factory.generator.deprecation.DeprecatedForRemovalBean;
import org.springframework.beans.testfixture.beans.factory.generator.deprecation.DeprecatedForRemovalConstructor;
import org.springframework.beans.testfixture.beans.factory.generator.deprecation.DeprecatedForRemovalMemberConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.deprecation.DeprecatedMemberConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import org.springframework.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import org.springframework.beans.testfixture.beans.factory.generator.injection.InjectionComponent;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link InstanceSupplierCodeGenerator}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class InstanceSupplierCodeGeneratorTests {

	private final TestGenerationContext generationContext;

	private final DefaultListableBeanFactory beanFactory;


	InstanceSupplierCodeGeneratorTests() {
		this.generationContext = new TestGenerationContext();
		this.beanFactory = new DefaultListableBeanFactory();
	}


	@Test
	void generateWhenHasDefaultConstructor() {
		BeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			TestBean bean = getBean(beanDefinition, instanceSupplier);
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
		this.beanFactory.registerSingleton("injected", "injected");
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			InjectionComponent bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(InjectionComponent.class).extracting("bean").isEqualTo("injected");
		});
		assertThat(getReflectionHints().getTypeHint(InjectionComponent.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasConstructorWithInnerClassAndDefaultConstructor() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(NoDependencyComponent.class);
		this.beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			NoDependencyComponent bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(NoDependencyComponent.class);
			assertThat(compiled.getSourceFile()).contains(
					"getBeanFactory().getBean(InnerComponentConfiguration.class).new NoDependencyComponent()");
		});
		assertThat(getReflectionHints().getTypeHint(NoDependencyComponent.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasConstructorWithInnerClassAndParameter() {
		BeanDefinition beanDefinition = new RootBeanDefinition(EnvironmentAwareComponent.class);
		this.beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
		this.beanFactory.registerSingleton("environment", new StandardEnvironment());
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			EnvironmentAwareComponent bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(EnvironmentAwareComponent.class);
			assertThat(compiled.getSourceFile()).contains(
					"getBeanFactory().getBean(InnerComponentConfiguration.class).new EnvironmentAwareComponent(");
		});
		assertThat(getReflectionHints().getTypeHint(EnvironmentAwareComponent.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasConstructorWithGeneric() {
		BeanDefinition beanDefinition = new RootBeanDefinition(NumberHolderFactoryBean.class);
		this.beanFactory.registerSingleton("number", 123);
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			NumberHolder<?> bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(NumberHolder.class);
			assertThat(bean).extracting("number").isNull(); // No property actually set
			assertThat(compiled.getSourceFile()).contains("NumberHolderFactoryBean::new");
		});
		assertThat(getReflectionHints().getTypeHint(NumberHolderFactoryBean.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasPrivateConstructor() {
		BeanDefinition beanDefinition = new RootBeanDefinition(TestBeanWithPrivateConstructor.class);
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			TestBeanWithPrivateConstructor bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(TestBeanWithPrivateConstructor.class);
			assertThat(compiled.getSourceFile())
					.contains("return BeanInstanceSupplier.<TestBeanWithPrivateConstructor>forConstructor();");
		});
		assertThat(getReflectionHints().getTypeHint(TestBeanWithPrivateConstructor.class))
				.satisfies(hasConstructorWithMode(ExecutableMode.INVOKE));
	}

	@Test
	void generateWhenHasFactoryMethodWithNoArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(String.class)
				.setFactoryMethodOnBean("stringBean", "config").getBeanDefinition();
		this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			String bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(String.class);
			assertThat(bean).isEqualTo("Hello");
			assertThat(compiled.getSourceFile()).contains(
					"getBeanFactory().getBean(SimpleConfiguration.class).stringBean()");
		});
		assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
				.satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasFactoryMethodOnInterface() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(SimpleBean.class)
				.setFactoryMethodOnBean("simpleBean", "config").getBeanDefinition();
		this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.rootBeanDefinition(DefaultSimpleBeanContract.class).getBeanDefinition());
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			Object bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(SimpleBean.class);
			assertThat(compiled.getSourceFile()).contains(
					"getBeanFactory().getBean(DefaultSimpleBeanContract.class).simpleBean()");
		});
		assertThat(getReflectionHints().getTypeHint(SimpleBeanContract.class))
				.satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
	}

	@Test
	void generateWhenHasPrivateStaticFactoryMethodWithNoArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(String.class)
				.setFactoryMethodOnBean("privateStaticStringBean", "config")
				.getBeanDefinition();
		this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			String bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(String.class);
			assertThat(bean).isEqualTo("Hello");
			assertThat(compiled.getSourceFile())
					.contains("forFactoryMethod")
					.doesNotContain("withGenerator");
		});
		assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
				.satisfies(hasMethodWithMode(ExecutableMode.INVOKE));
	}

	@Test
	void generateWhenHasStaticFactoryMethodWithNoArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(Integer.class)
				.setFactoryMethodOnBean("integerBean", "config").getBeanDefinition();
		this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			Integer bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(Integer.class);
			assertThat(bean).isEqualTo(42);
			assertThat(compiled.getSourceFile())
					.contains("(registeredBean) -> SimpleConfiguration.integerBean()");
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
		this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SampleFactory.class).getBeanDefinition());
		this.beanFactory.registerSingleton("number", 42);
		this.beanFactory.registerSingleton("string", "test");
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			String bean = getBean(beanDefinition, instanceSupplier);
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
		this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
				.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		compile(beanDefinition, (instanceSupplier, compiled) -> {
			Integer bean = getBean(beanDefinition, instanceSupplier);
			assertThat(bean).isInstanceOf(Integer.class);
			assertThat(bean).isEqualTo(42);
			assertThat(compiled.getSourceFile()).doesNotContain(") throws Exception {");
		});
		assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
				.satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
	}

	@Nested
	@SuppressWarnings("deprecation")
	class DeprecationTests {

		private static final TestCompiler TEST_COMPILER = TestCompiler.forSystem()
				.withCompilerOptions("-Xlint:all", "-Xlint:-rawtypes", "-Werror");

		@Test
		@Disabled("Need to move to a separate method so that the warning can be suppressed")
		void generateWhenTargetClassIsDeprecated() {
			compileAndCheckWarnings(new RootBeanDefinition(DeprecatedBean.class));
		}

		@Test
		void generateWhenTargetConstructorIsDeprecated() {
			compileAndCheckWarnings(new RootBeanDefinition(DeprecatedConstructor.class));
		}

		@Test
		void generateWhenTargetFactoryMethodIsDeprecated() {
			BeanDefinition beanDefinition = BeanDefinitionBuilder
					.rootBeanDefinition(String.class)
					.setFactoryMethodOnBean("deprecatedString", "config").getBeanDefinition();
			beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
					.genericBeanDefinition(DeprecatedMemberConfiguration.class).getBeanDefinition());
			compileAndCheckWarnings(beanDefinition);
		}

		@Test
		void generateWhenTargetFactoryMethodParameterIsDeprecated() {
			BeanDefinition beanDefinition = BeanDefinitionBuilder
					.rootBeanDefinition(String.class)
					.setFactoryMethodOnBean("deprecatedParameter", "config").getBeanDefinition();
			beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
					.genericBeanDefinition(DeprecatedMemberConfiguration.class).getBeanDefinition());
			beanFactory.registerBeanDefinition("parameter", new RootBeanDefinition(DeprecatedBean.class));
			compileAndCheckWarnings(beanDefinition);
		}

		@Test
		void generateWhenTargetFactoryMethodReturnTypeIsDeprecated() {
			BeanDefinition beanDefinition = BeanDefinitionBuilder
					.rootBeanDefinition(DeprecatedBean.class)
					.setFactoryMethodOnBean("deprecatedReturnType", "config").getBeanDefinition();
			beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
					.genericBeanDefinition(DeprecatedMemberConfiguration.class).getBeanDefinition());
			compileAndCheckWarnings(beanDefinition);
		}

		private void compileAndCheckWarnings(BeanDefinition beanDefinition) {
			assertThatNoException().isThrownBy(() -> compile(TEST_COMPILER, beanDefinition,
					((instanceSupplier, compiled) -> {})));
		}

	}

	@Nested
	@SuppressWarnings("removal")
	class DeprecationForRemovalTests {

		private static final TestCompiler TEST_COMPILER = TestCompiler.forSystem()
				.withCompilerOptions("-Xlint:all", "-Xlint:-rawtypes", "-Werror");

		@Test
		@Disabled("Need to move to a separate method so that the warning can be suppressed")
		void generateWhenTargetClassIsDeprecatedForRemoval() {
			compileAndCheckWarnings(new RootBeanDefinition(DeprecatedForRemovalBean.class));
		}

		@Test
		void generateWhenTargetConstructorIsDeprecatedForRemoval() {
			compileAndCheckWarnings(new RootBeanDefinition(DeprecatedForRemovalConstructor.class));
		}

		@Test
		void generateWhenTargetFactoryMethodIsDeprecatedForRemoval() {
			BeanDefinition beanDefinition = BeanDefinitionBuilder
					.rootBeanDefinition(String.class)
					.setFactoryMethodOnBean("deprecatedString", "config").getBeanDefinition();
			beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
					.genericBeanDefinition(DeprecatedForRemovalMemberConfiguration.class).getBeanDefinition());
			compileAndCheckWarnings(beanDefinition);
		}

		@Test
		void generateWhenTargetFactoryMethodParameterIsDeprecatedForRemoval() {
			BeanDefinition beanDefinition = BeanDefinitionBuilder
					.rootBeanDefinition(String.class)
					.setFactoryMethodOnBean("deprecatedParameter", "config").getBeanDefinition();
			beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
					.genericBeanDefinition(DeprecatedForRemovalMemberConfiguration.class).getBeanDefinition());
			beanFactory.registerBeanDefinition("parameter", new RootBeanDefinition(DeprecatedForRemovalBean.class));
			compileAndCheckWarnings(beanDefinition);
		}

		private void compileAndCheckWarnings(BeanDefinition beanDefinition) {
			assertThatNoException().isThrownBy(() -> compile(TEST_COMPILER, beanDefinition,
					((instanceSupplier, compiled) -> {})));
		}

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
		return hint -> assertThat(hint.getMode()).isEqualTo(mode);
	}

	@SuppressWarnings("unchecked")
	private <T> T getBean(BeanDefinition beanDefinition, InstanceSupplier<?> instanceSupplier) {
		((RootBeanDefinition) beanDefinition).setInstanceSupplier(instanceSupplier);
		this.beanFactory.registerBeanDefinition("testBean", beanDefinition);
		return (T) this.beanFactory.getBean("testBean");
	}

	private void compile(BeanDefinition beanDefinition, BiConsumer<InstanceSupplier<?>, Compiled> result) {
		compile(TestCompiler.forSystem(), beanDefinition, result);
	}

	private void compile(TestCompiler testCompiler, BeanDefinition beanDefinition,
			BiConsumer<InstanceSupplier<?>, Compiled> result) {

		DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory(this.beanFactory);
		freshBeanFactory.registerBeanDefinition("testBean", beanDefinition);
		RegisteredBean registeredBean = RegisteredBean.of(freshBeanFactory, "testBean");
		DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
		GeneratedClass generateClass = this.generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
		InstanceSupplierCodeGenerator generator = new InstanceSupplierCodeGenerator(
				this.generationContext, generateClass.getName(),
				generateClass.getMethods(), false);
		InstantiationDescriptor instantiationDescriptor = registeredBean.resolveInstantiationDescriptor();
		assertThat(instantiationDescriptor).isNotNull();
		CodeBlock generatedCode = generator.generateCode(registeredBean, instantiationDescriptor);
		typeBuilder.set(type -> {
			type.addModifiers(Modifier.PUBLIC);
			type.addSuperinterface(ParameterizedTypeName.get(Supplier.class, InstanceSupplier.class));
			type.addMethod(MethodSpec.methodBuilder("get")
					.addModifiers(Modifier.PUBLIC)
					.returns(InstanceSupplier.class)
					.addStatement("return $L", generatedCode).build());
		});
		this.generationContext.writeGeneratedContent();
		testCompiler.with(this.generationContext).compile(compiled -> result.accept(
				(InstanceSupplier<?>) compiled.getInstance(Supplier.class).get(), compiled));
	}

}
