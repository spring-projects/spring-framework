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

package org.springframework.beans.factory.generator;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponent;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponent;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import org.springframework.beans.testfixture.beans.factory.generator.injection.InjectionComponent;
import org.springframework.beans.testfixture.beans.factory.generator.property.ConfigurableBean;
import org.springframework.beans.testfixture.beans.factory.generator.visibility.ProtectedConstructorComponent;
import org.springframework.beans.testfixture.beans.factory.generator.visibility.ProtectedFactoryMethod;
import org.springframework.core.env.Environment;
import org.springframework.core.testfixture.aot.generator.visibility.PublicFactoryBean;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.support.CodeSnippet;
import org.springframework.javapoet.support.MultiStatement;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link BeanRegistrationBeanFactoryContribution}.
 *
 * @author Stephane Nicoll
 */
class BeanRegistrationBeanFactoryContributionTests {

	private final DefaultGeneratedTypeContext generatedTypeContext = new DefaultGeneratedTypeContext("com.example", packageName -> GeneratedType.of(ClassName.get(packageName, "Test")));

	private final BeanFactoryInitialization initialization = new BeanFactoryInitialization(this.generatedTypeContext);

	@Test
	void generateUsingConstructor() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(InjectionComponent.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(InjectionComponent.class), code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", InjectionComponent.class).withConstructor(String.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingConstructorWithNoArgument() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SimpleConfiguration.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(SimpleConfiguration.class), code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingConstructorOnInnerClass() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(EnvironmentAwareComponent.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(EnvironmentAwareComponent.class), code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", InnerComponentConfiguration.EnvironmentAwareComponent.class).withConstructor(InnerComponentConfiguration.class, Environment.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingConstructorOnInnerClassWithNoExtraArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(NoDependencyComponent.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, singleConstructor(NoDependencyComponent.class), code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", InnerComponentConfiguration.NoDependencyComponent.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingFactoryMethod() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, method(SampleFactory.class, "create", String.class), code -> code.add("() -> test"));
		assertThat(registration.hasImport(SampleFactory.class)).isTrue();
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", String.class).withFactoryMethod(SampleFactory.class, "create", String.class)
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingFactoryMethodWithNoArgument() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(Integer.class).getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, method(SampleFactory.class, "integerBean"), code -> code.add("() -> test"));
		assertThat(registration.hasImport(SampleFactory.class)).isTrue();
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", Integer.class).withFactoryMethod(SampleFactory.class, "integerBean")
						.instanceSupplier(() -> test).register(beanFactory);
				""");
	}

	@Test
	void generateUsingPublicAccessDoesNotAccessAnotherPackage() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SimpleConfiguration.class).getBeanDefinition();
		getContribution(beanDefinition, singleConstructor(SimpleConfiguration.class)).applyTo(this.initialization);
		assertThat(this.generatedTypeContext.toJavaFiles()).hasSize(1);
		assertThat(CodeSnippet.of(this.initialization.toCodeBlock()).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(SimpleConfiguration::new).register(beanFactory);
				""");
	}

	@Test
	void generateUsingProtectedConstructorWritesToBlessedPackage() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(ProtectedConstructorComponent.class).getBeanDefinition();
		getContribution(beanDefinition, singleConstructor(ProtectedConstructorComponent.class)).applyTo(this.initialization);
		assertThat(this.generatedTypeContext.hasGeneratedType(ProtectedConstructorComponent.class.getPackageName())).isTrue();
		GeneratedType generatedType = this.generatedTypeContext.getGeneratedType(ProtectedConstructorComponent.class.getPackageName());
		assertThat(removeIndent(codeOf(generatedType), 1)).containsSequence("""
				public static void registerTest(DefaultListableBeanFactory beanFactory) {
					BeanDefinitionRegistrar.of("test", ProtectedConstructorComponent.class)
							.instanceSupplier(ProtectedConstructorComponent::new).register(beanFactory);
				}""");
		assertThat(CodeSnippet.of(this.initialization.toCodeBlock()).getSnippet()).isEqualTo(
				ProtectedConstructorComponent.class.getPackageName() + ".Test.registerTest(beanFactory);\n");
	}

	@Test
	void generateUsingProtectedFactoryMethodWritesToBlessedPackage() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
		getContribution(beanDefinition, method(ProtectedFactoryMethod.class, "testBean", Integer.class))
				.applyTo(this.initialization);
		assertThat(this.generatedTypeContext.hasGeneratedType(ProtectedFactoryMethod.class.getPackageName())).isTrue();
		GeneratedType generatedType = this.generatedTypeContext.getGeneratedType(ProtectedConstructorComponent.class.getPackageName());
		assertThat(removeIndent(codeOf(generatedType), 1)).containsSequence("""
				public static void registerProtectedFactoryMethod_test(DefaultListableBeanFactory beanFactory) {
					BeanDefinitionRegistrar.of("test", String.class).withFactoryMethod(ProtectedFactoryMethod.class, "testBean", Integer.class)
							.instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> beanFactory.getBean(ProtectedFactoryMethod.class).testBean(attributes.get(0)))).register(beanFactory);
				}""");
		assertThat(CodeSnippet.of(this.initialization.toCodeBlock()).getSnippet()).isEqualTo(
				ProtectedConstructorComponent.class.getPackageName() + ".Test.registerProtectedFactoryMethod_test(beanFactory);\n");
	}

	@Test
	void generateUsingProtectedGenericTypeWritesToBlessedPackage() {
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder.rootBeanDefinition(
				PublicFactoryBean.class).getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, String.class);
		// This resolve the generic parameter to a protected type
		beanDefinition.setTargetType(PublicFactoryBean.resolveToProtectedGenericParameter());
		getContribution(beanDefinition, singleConstructor(PublicFactoryBean.class)).applyTo(this.initialization);
		assertThat(this.generatedTypeContext.hasGeneratedType(PublicFactoryBean.class.getPackageName())).isTrue();
		GeneratedType generatedType = this.generatedTypeContext.getGeneratedType(PublicFactoryBean.class.getPackageName());
		assertThat(removeIndent(codeOf(generatedType), 1)).containsSequence("""
				public static void registerTest(DefaultListableBeanFactory beanFactory) {
					BeanDefinitionRegistrar.of("test", ResolvableType.forClassWithGenerics(PublicFactoryBean.class, ProtectedType.class)).withConstructor(Class.class)
							.instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new PublicFactoryBean(attributes.get(0)))).customize((bd) -> bd.getConstructorArgumentValues().addIndexedArgumentValue(0, String.class)).register(beanFactory);
				}""");
		assertThat(CodeSnippet.of(this.initialization.toCodeBlock()).getSnippet()).isEqualTo(
				PublicFactoryBean.class.getPackageName() + ".Test.registerTest(beanFactory);\n");
	}

	@Test
	void generateWithBeanDefinitionHavingSyntheticFlag() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setSynthetic(true)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setSynthetic(true)).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingDependsOn() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setDependsOn("test")).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setDependsOn(new String[] { "test" })).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingLazyInit() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setLazyInit(true)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setLazyInit(true)).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingRole() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setRole(2)).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingScope() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setScope("prototype")).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingAutowiredCandidate() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setAutowireCandidate(false)).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.setAutowireCandidate(false)).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingDefaultAutowiredCandidateDoesNotConfigureIt() {
		assertThat(simpleConfigurationRegistration(bd -> bd.setAutowireCandidate(true)).getSnippet())
				.doesNotContain("bd.setAutowireCandidate(");
	}

	@Test
	void generateWithBeanDefinitionHavingMultipleAttributes() {
		assertThat(simpleConfigurationRegistration(bd -> {
			bd.setSynthetic(true);
			bd.setPrimary(true);
		}).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> {
					bd.setPrimary(true);
					bd.setSynthetic(true);
				}).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingProperty() {
		assertThat(simpleConfigurationRegistration(bd -> bd.getPropertyValues().addPropertyValue("test", "Hello")).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.getPropertyValues().addPropertyValue("test", "Hello")).register(beanFactory);
				""");
	}

	@Test
	void generateWithBeanDefinitionHavingSeveralProperties() {
		CodeSnippet registration = simpleConfigurationRegistration(bd -> {
			bd.getPropertyValues().addPropertyValue("test", "Hello");
			bd.getPropertyValues().addPropertyValue("counter", 42);
		});
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> {
					MutablePropertyValues propertyValues = bd.getPropertyValues();
					propertyValues.addPropertyValue("test", "Hello");
					propertyValues.addPropertyValue("counter", 42);
				}).register(beanFactory);
				""");
		assertThat(registration.hasImport(MutablePropertyValues.class)).isTrue();
	}

	@Test
	void generateWithBeanDefinitionHavingPropertyReference() {
		CodeSnippet registration = simpleConfigurationRegistration(bd -> bd.getPropertyValues()
				.addPropertyValue("myService", new RuntimeBeanReference("test")));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", SimpleConfiguration.class)
						.instanceSupplier(() -> SimpleConfiguration::new).customize((bd) -> bd.getPropertyValues().addPropertyValue("myService", new RuntimeBeanReference("test"))).register(beanFactory);
				""");
		assertThat(registration.hasImport(RuntimeBeanReference.class)).isTrue();
	}

	@Test
	void generateWithBeanDefinitionHavingPropertyAsBeanDefinition() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition innerBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SimpleConfiguration.class, "stringBean")
				.getBeanDefinition();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(ConfigurableBean.class)
				.addPropertyValue("name", innerBeanDefinition).getBeanDefinition();
		getContribution(beanFactory, beanDefinition).applyTo(this.initialization);
		CodeSnippet registration = CodeSnippet.of(this.initialization.toCodeBlock());
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", ConfigurableBean.class)
						.instanceSupplier(ConfigurableBean::new).customize((bd) -> bd.getPropertyValues().addPropertyValue("name", BeanDefinitionRegistrar.inner(SimpleConfiguration.class).withFactoryMethod(SimpleConfiguration.class, "stringBean")
						.instanceSupplier(() -> beanFactory.getBean(SimpleConfiguration.class).stringBean()).toBeanDefinition())).register(beanFactory);
				""");
		assertThat(registration.hasImport(SimpleConfiguration.class)).isTrue();
	}

	@Test
	void generateWithBeanDefinitionHavingPropertyAsListOfBeanDefinitions() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition innerBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SimpleConfiguration.class, "stringBean")
				.getBeanDefinition();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(ConfigurableBean.class)
				.addPropertyValue("names", List.of(innerBeanDefinition, innerBeanDefinition)).getBeanDefinition();
		getContribution(beanFactory, beanDefinition).applyTo(this.initialization);
		CodeSnippet registration = CodeSnippet.of(this.initialization.toCodeBlock());
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", ConfigurableBean.class)
						.instanceSupplier(ConfigurableBean::new).customize((bd) -> bd.getPropertyValues().addPropertyValue("names", List.of(BeanDefinitionRegistrar.inner(SimpleConfiguration.class).withFactoryMethod(SimpleConfiguration.class, "stringBean")
						.instanceSupplier(() -> beanFactory.getBean(SimpleConfiguration.class).stringBean()).toBeanDefinition(), BeanDefinitionRegistrar.inner(SimpleConfiguration.class).withFactoryMethod(SimpleConfiguration.class, "stringBean")
						.instanceSupplier(() -> beanFactory.getBean(SimpleConfiguration.class).stringBean()).toBeanDefinition()))).register(beanFactory);
				""");
		assertThat(registration.hasImport(SimpleConfiguration.class)).isTrue();
	}

	@Test
	void generateWithBeanDefinitionHavingPropertyAsBeanDefinitionUseDedicatedVariableNames() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition innerBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SimpleConfiguration.class, "stringBean")
				.setRole(2).getBeanDefinition();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(ConfigurableBean.class)
				.addPropertyValue("name", innerBeanDefinition).getBeanDefinition();
		getContribution(beanFactory, beanDefinition).applyTo(this.initialization);
		CodeSnippet registration = CodeSnippet.of(this.initialization.toCodeBlock());
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", ConfigurableBean.class)
						.instanceSupplier(ConfigurableBean::new).customize((bd) -> bd.getPropertyValues().addPropertyValue("name", BeanDefinitionRegistrar.inner(SimpleConfiguration.class).withFactoryMethod(SimpleConfiguration.class, "stringBean")
						.instanceSupplier(() -> beanFactory.getBean(SimpleConfiguration.class).stringBean()).customize((bd_) -> bd_.setRole(2)).toBeanDefinition())).register(beanFactory);
				""");
		assertThat(registration.hasImport(SimpleConfiguration.class)).isTrue();
	}

	@Test
	void generateUsingSingleConstructorArgument() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(String.class).getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, "hello");
		CodeSnippet registration = beanRegistration(beanDefinition, method(SampleFactory.class, "create", String.class),
				code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", String.class).withFactoryMethod(SampleFactory.class, "create", String.class)
						.instanceSupplier(() -> test).customize((bd) -> bd.getConstructorArgumentValues().addIndexedArgumentValue(0, "hello")).register(beanFactory);
				""");
	}

	@Test
	void generateUsingSeveralConstructorArguments() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(String.class)
				.addConstructorArgValue(42).addConstructorArgReference("testBean")
				.getBeanDefinition();
		CodeSnippet registration = beanRegistration(beanDefinition, method(SampleFactory.class, "create", Number.class, String.class),
				code -> code.add("() -> test"));
		assertThat(registration.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", String.class).withFactoryMethod(SampleFactory.class, "create", Number.class, String.class)
						.instanceSupplier(() -> test).customize((bd) -> {
					ConstructorArgumentValues argumentValues = bd.getConstructorArgumentValues();
					argumentValues.addIndexedArgumentValue(0, 42);
					argumentValues.addIndexedArgumentValue(1, new RuntimeBeanReference("testBean"));
				}).register(beanFactory);
				""");
		assertThat(registration.hasImport(ConstructorArgumentValues.class)).isTrue();
	}

	@Test
	void registerRuntimeHintsWithNoPropertyValuesDoesNotAccessRuntimeHints() {
		RootBeanDefinition bd = new RootBeanDefinition(String.class);
		RuntimeHints runtimeHints = mock(RuntimeHints.class);
		getContribution(new DefaultListableBeanFactory(), bd).registerRuntimeHints(runtimeHints);
		verifyNoInteractions(runtimeHints);
	}

	@Test
	void registerRuntimeHintsWithInvalidProperty() {
		BeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition(ConfigurableBean.class)
				.addPropertyValue("notAProperty", "invalid").addPropertyValue("name", "hello")
				.getBeanDefinition();
		RuntimeHints runtimeHints = new RuntimeHints();
		getContribution(new DefaultListableBeanFactory(), bd).registerRuntimeHints(runtimeHints);
		assertThat(runtimeHints.reflection().getTypeHint(ConfigurableBean.class)).satisfies(hint -> {
			assertThat(hint.fields()).isEmpty();
			assertThat(hint.constructors()).isEmpty();
			assertThat(hint.methods()).singleElement().satisfies(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setName");
				assertThat(methodHint.getParameterTypes()).containsExactly(TypeReference.of(String.class));
				assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
			});
			assertThat(hint.getMemberCategories()).isEmpty();
		});
	}

	@Test
	void registerRuntimeHintsForPropertiesUseDeclaringClass() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("environment", mock(Environment.class));
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(IntegerFactoryBean.class)
				.addConstructorArgReference("environment")
				.addPropertyValue("name", "Hello").getBeanDefinition();
		getContribution(beanFactory, beanDefinition).applyTo(this.initialization);
		ReflectionHints reflectionHints = this.initialization.generatedTypeContext().runtimeHints().reflection();
		assertThat(reflectionHints.typeHints()).anySatisfy(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(BaseFactoryBean.class));
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).singleElement()
					.satisfies(methodHint("setName", String.class));
			assertThat(typeHint.fields()).isEmpty();
		}).anySatisfy(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(IntegerFactoryBean.class));
			assertThat(typeHint.constructors()).singleElement()
					.satisfies(constructorHint(Environment.class));
			assertThat(typeHint.methods()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
		}).hasSize(2);
	}


	@Test
	void registerRuntimeHintsForProperties() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(NameAndCountersComponent.class)
				.addPropertyValue("name", "Hello").addPropertyValue("counter", 42).getBeanDefinition();
		getContribution(new DefaultListableBeanFactory(), beanDefinition).applyTo(this.initialization);
		ReflectionHints reflectionHints = this.initialization.generatedTypeContext().runtimeHints().reflection();
		assertThat(reflectionHints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(NameAndCountersComponent.class));
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).anySatisfy(methodHint("setName", String.class))
					.anySatisfy(methodHint("setCounter", Integer.class)).hasSize(2);
			assertThat(typeHint.fields()).isEmpty();
		});
	}


	@Test
	void registerReflectionEntriesForInnerBeanDefinition() {
		AbstractBeanDefinition innerBd = BeanDefinitionBuilder.rootBeanDefinition(IntegerFactoryBean.class)
				.addPropertyValue("name", "test").getBeanDefinition();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(NameAndCountersComponent.class)
				.addPropertyValue("counter", innerBd).getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("environment", Environment.class);
		getContribution(beanFactory, beanDefinition).applyTo(this.initialization);
		ReflectionHints reflectionHints = this.initialization.generatedTypeContext().runtimeHints().reflection();
		assertThat(reflectionHints.typeHints()).anySatisfy(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(NameAndCountersComponent.class));
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint("setCounter", Integer.class));
			assertThat(typeHint.fields()).isEmpty();
		}).anySatisfy(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(BaseFactoryBean.class));
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint("setName", String.class));
		}).anySatisfy(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(IntegerFactoryBean.class));
			assertThat(typeHint.constructors()).singleElement().satisfies(constructorHint(Environment.class));
		}).hasSize(3);
	}

	@Test
	void registerReflectionEntriesForListOfInnerBeanDefinition() {
		AbstractBeanDefinition innerBd1 = BeanDefinitionBuilder.rootBeanDefinition(IntegerFactoryBean.class)
				.addPropertyValue("name", "test").getBeanDefinition();
		AbstractBeanDefinition innerBd2 = BeanDefinitionBuilder.rootBeanDefinition(AnotherIntegerFactoryBean.class)
				.addPropertyValue("name", "test").getBeanDefinition();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(NameAndCountersComponent.class)
				.addPropertyValue("counters", List.of(innerBd1, innerBd2)).getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("environment", Environment.class);
		getContribution(beanFactory, beanDefinition).applyTo(this.initialization);
		ReflectionHints reflectionHints = this.initialization.generatedTypeContext().runtimeHints().reflection();
		assertThat(reflectionHints.typeHints()).anySatisfy(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(NameAndCountersComponent.class));
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint("setCounters", List.class));
			assertThat(typeHint.fields()).isEmpty();
		}).anySatisfy(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(BaseFactoryBean.class));
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint("setName", String.class));
		}).anySatisfy(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(IntegerFactoryBean.class));
			assertThat(typeHint.constructors()).singleElement().satisfies(constructorHint(Environment.class));
		}).anySatisfy(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(AnotherIntegerFactoryBean.class));
			assertThat(typeHint.constructors()).singleElement().satisfies(constructorHint(Environment.class));
		}).hasSize(4);
	}

	private Consumer<ExecutableHint> methodHint(String name, Class<?>... parameterTypes) {
		return executableHint -> {
			assertThat(executableHint.getName()).isEqualTo(name);
			assertThat(executableHint.getParameterTypes()).containsExactly(Arrays.stream(parameterTypes)
					.map(TypeReference::of).toArray(TypeReference[]::new));
		};
	}

	private Consumer<ExecutableHint> constructorHint(Class<?>... parameterTypes) {
		return methodHint("<init>", parameterTypes);
	}


	private CodeSnippet simpleConfigurationRegistration(Consumer<RootBeanDefinition> bd) {
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(SimpleConfiguration.class).getBeanDefinition();
		bd.accept(beanDefinition);
		return beanRegistration(beanDefinition, singleConstructor(SimpleConfiguration.class),
				code -> code.add("() -> SimpleConfiguration::new"));
	}

	private BeanRegistrationBeanFactoryContribution getContribution(DefaultListableBeanFactory beanFactory, BeanDefinition beanDefinition) {
		BeanRegistrationBeanFactoryContribution contribution = new DefaultBeanRegistrationContributionProvider(beanFactory)
				.getContributionFor("test", (RootBeanDefinition) beanDefinition);
		assertThat(contribution).isNotNull();
		return contribution;
	}

	private BeanFactoryContribution getContribution(BeanDefinition beanDefinition, Executable instanceCreator) {
		return new BeanRegistrationBeanFactoryContribution("test", beanDefinition,
				new DefaultBeanInstantiationGenerator(instanceCreator, Collections.emptyList()));
	}

	private CodeSnippet beanRegistration(BeanDefinition beanDefinition, Executable instanceCreator, Consumer<Builder> instanceSupplier) {
		BeanRegistrationBeanFactoryContribution generator = new BeanRegistrationBeanFactoryContribution("test", beanDefinition,
				new DefaultBeanInstantiationGenerator(instanceCreator, Collections.emptyList()));
		return CodeSnippet.of(generator.generateBeanRegistration(new RuntimeHints(),
				toMultiStatements(instanceSupplier)));
	}

	private Constructor<?> singleConstructor(Class<?> type) {
		return type.getDeclaredConstructors()[0];
	}

	private Method method(Class<?> type, String name, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(type, name, parameterTypes);
		assertThat(method).isNotNull();
		return method;
	}

	private MultiStatement toMultiStatements(Consumer<Builder> instanceSupplier) {
		Builder code = CodeBlock.builder();
		instanceSupplier.accept(code);
		MultiStatement statements = new MultiStatement();
		statements.add(code.build());
		return statements;
	}

	private String codeOf(GeneratedType type) {
		try {
			StringWriter out = new StringWriter();
			type.toJavaFile().writeTo(out);
			return out.toString();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private String removeIndent(String content, int indent) {
		return content.lines().map(line -> {
			for (int i = 0; i < indent; i++) {
				if (line.startsWith("\t")) {
					line = line.substring(1);
				}
			}
			return line;
		}).collect(Collectors.joining("\n"));
	}

	static abstract class BaseFactoryBean {

		public void setName(String name) {

		}

	}

	@SuppressWarnings("unused")
	static class IntegerFactoryBean extends BaseFactoryBean implements FactoryBean<Integer> {

		public IntegerFactoryBean(Environment environment) {

		}

		@Override
		public Class<?> getObjectType() {
			return Integer.class;
		}

		@Override
		public Integer getObject() {
			return 42;
		}

	}

	@SuppressWarnings("unused")
	static class AnotherIntegerFactoryBean extends IntegerFactoryBean {

		public AnotherIntegerFactoryBean(Environment environment) {
			super(environment);
		}

	}

	static class NameAndCountersComponent {

		private String name;

		private List<Integer> counters;

		public void setName(String name) {
			this.name = name;
		}

		public void setCounter(Integer counter) {
			setCounters(List.of(counter));
		}

		public void setCounters(List<Integer> counters) {
			this.counters = counters;
		}
	}

}
