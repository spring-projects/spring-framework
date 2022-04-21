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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponent;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponent;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import org.springframework.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import org.springframework.beans.testfixture.beans.factory.generator.injection.InjectionComponent;
import org.springframework.beans.testfixture.beans.factory.generator.visibility.ProtectedConstructorComponent;
import org.springframework.beans.testfixture.beans.factory.generator.visibility.ProtectedFactoryMethod;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.support.CodeSnippet;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultBeanInstantiationGenerator}.
 *
 * @author Stephane Nicoll
 */
class DefaultBeanInstantiationGeneratorTests {

	@Test
	void generateUsingDefaultConstructorUsesMethodReference() {
		CodeContribution contribution = generate(SimpleConfiguration.class.getDeclaredConstructors()[0]);
		assertThat(code(contribution)).isEqualTo("SimpleConfiguration::new");
		assertThat(reflectionHints(contribution, SimpleConfiguration.class)).isNull();
	}

	@Test
	void generateUsingConstructorWithoutParameterAndMultipleCandidatesDoesNotUseMethodReference() throws NoSuchMethodException {
		CodeContribution contribution = generate(TestBean.class.getConstructor());
		assertThat(code(contribution)).isEqualTo("() -> new TestBean()");
		assertThat(reflectionHints(contribution, TestBean.class)).isNull();
	}

	@Test
	void generateUsingConstructorWithParameter() {
		Constructor<?> constructor = InjectionComponent.class.getDeclaredConstructors()[0];
		CodeContribution contribution = generate(constructor);
		assertThat(code(contribution).lines()).containsOnly(
				"(instanceContext) -> instanceContext.create(beanFactory, (attributes) -> "
						+ "new InjectionComponent(attributes.get(0)))");
		assertThat(reflectionHints(contribution, InjectionComponent.class))
				.satisfies(hasSingleQueryConstructor(constructor));
	}

	@Test
	void generateUsingConstructorWithInnerClassAndNoExtraArg() {
		CodeContribution contribution = generate(NoDependencyComponent.class.getDeclaredConstructors()[0]);
		assertThat(code(contribution).lines()).containsOnly(
				"() -> beanFactory.getBean(InnerComponentConfiguration.class).new NoDependencyComponent()");
		assertThat(reflectionHints(contribution, NoDependencyComponent.class)).isNull();
	}

	@Test
	void generateUsingConstructorWithInnerClassAndExtraArg() {
		Constructor<?> constructor = EnvironmentAwareComponent.class.getDeclaredConstructors()[0];
		CodeContribution contribution = generate(constructor);
		assertThat(code(contribution).lines()).containsOnly(
				"(instanceContext) -> instanceContext.create(beanFactory, (attributes) -> "
						+ "beanFactory.getBean(InnerComponentConfiguration.class).new EnvironmentAwareComponent(attributes.get(1)))");
		assertThat(reflectionHints(contribution, EnvironmentAwareComponent.class))
				.satisfies(hasSingleQueryConstructor(constructor));
	}

	@Test
	void generateUsingConstructorOfTypeWithGeneric() {
		CodeContribution contribution = generate(NumberHolderFactoryBean.class.getDeclaredConstructors()[0]);
		assertThat(code(contribution)).isEqualTo("NumberHolderFactoryBean::new");
		assertThat(reflectionHints(contribution, NumberHolderFactoryBean.class)).isNull();
	}

	@Test
	void generateUsingNoArgConstructorAndContributionsDoesNotUseMethodReference() {
		CodeContribution contribution = generate(SimpleConfiguration.class.getDeclaredConstructors()[0],
				contrib -> contrib.statements().add(CodeBlock.of("// hello\n")),
				contrib -> {});
		assertThat(code(contribution)).isEqualTo("""
				(instanceContext) -> {
					SimpleConfiguration bean = new SimpleConfiguration();
					// hello
					return bean;
				}""");
	}

	@Test
	void generateUsingContributionsRegisterHints() {
		CodeContribution contribution = generate(SimpleConfiguration.class.getDeclaredConstructors()[0],
				contrib -> {
					contrib.statements().add(CodeBlock.of("// hello\n"));
					contrib.runtimeHints().resources().registerPattern("com/example/*.properties");
				},
				contrib -> contrib.runtimeHints().reflection().registerType(TypeReference.of(String.class),
						hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS)));
		assertThat(code(contribution)).isEqualTo("""
				(instanceContext) -> {
					SimpleConfiguration bean = new SimpleConfiguration();
					// hello
					return bean;
				}""");
		assertThat(contribution.runtimeHints().resources().resourcePatterns()).singleElement().satisfies(hint ->
				assertThat(hint.getIncludes()).containsOnly("com/example/*.properties"));
		assertThat(contribution.runtimeHints().reflection().getTypeHint(String.class)).satisfies(hint -> {
			assertThat(hint.getType()).isEqualTo(TypeReference.of(String.class));
			assertThat(hint.getMemberCategories()).containsOnly(MemberCategory.INVOKE_PUBLIC_METHODS);
		});
	}

	@Test
	void generateUsingMethodWithNoArg() {
		Method method = method(SimpleConfiguration.class, "stringBean");
		CodeContribution contribution = generate(method);
		assertThat(code(contribution)).isEqualTo("() -> beanFactory.getBean(SimpleConfiguration.class).stringBean()");
		assertThat(reflectionHints(contribution, SimpleConfiguration.class))
				.satisfies(hasSingleQueryMethod(method));
	}

	@Test
	void generateUsingStaticMethodWithNoArg() {
		Method method = method(SampleFactory.class, "integerBean");
		CodeContribution contribution = generate(method);
		assertThat(code(contribution)).isEqualTo("() -> SampleFactory.integerBean()");
		assertThat(reflectionHints(contribution, SampleFactory.class))
				.satisfies(hasSingleQueryMethod(method));
	}

	@Test
	void generateUsingMethodWithArg() {
		Method method = method(SampleFactory.class, "create", Number.class, String.class);
		CodeContribution contribution = generate(method);
		assertThat(code(contribution)).isEqualTo("(instanceContext) -> instanceContext.create(beanFactory, (attributes) -> "
				+ "SampleFactory.create(attributes.get(0), attributes.get(1)))");
		assertThat(reflectionHints(contribution, SampleFactory.class))
				.satisfies(hasSingleQueryMethod(method));
	}

	@Test
	void generateUsingMethodAndContributions() {
		CodeContribution contribution = generate(method(SimpleConfiguration.class, "stringBean"),
				contrib -> {
					contrib.statements().add(CodeBlock.of("// hello\n"));
					contrib.runtimeHints().resources().registerPattern("com/example/*.properties");
				},
				contrib -> contrib.runtimeHints().reflection().registerType(TypeReference.of(String.class),
						hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS)));
		assertThat(code(contribution)).isEqualTo("""
				(instanceContext) -> {
					String bean = beanFactory.getBean(SimpleConfiguration.class).stringBean();
					// hello
					return bean;
				}""");
		assertThat(contribution.runtimeHints().resources().resourcePatterns()).singleElement().satisfies(hint ->
				assertThat(hint.getIncludes()).containsOnly("com/example/*.properties"));
		assertThat(contribution.runtimeHints().reflection().getTypeHint(String.class)).satisfies(hint -> {
			assertThat(hint.getType()).isEqualTo(TypeReference.of(String.class));
			assertThat(hint.getMemberCategories()).containsOnly(MemberCategory.INVOKE_PUBLIC_METHODS);
		});
	}

	@Test
	void generateUsingProtectedConstructorRegistersProtectedAccess() {
		CodeContribution contribution = generate(ProtectedConstructorComponent.class.getDeclaredConstructors()[0]);
		assertThat(contribution.protectedAccess().isAccessible("com.example")).isFalse();
		assertThat(contribution.protectedAccess().getPrivilegedPackageName("com.example"))
				.isEqualTo(ProtectedConstructorComponent.class.getPackageName());
	}

	@Test
	void generateUsingProtectedMethodRegistersProtectedAccess() {
		CodeContribution contribution = generate(method(ProtectedFactoryMethod.class, "testBean", Integer.class));
		assertThat(contribution.protectedAccess().isAccessible("com.example")).isFalse();
		assertThat(contribution.protectedAccess().getPrivilegedPackageName("com.example"))
				.isEqualTo(ProtectedFactoryMethod.class.getPackageName());
	}

	private String code(CodeContribution contribution) {
		return CodeSnippet.process(contribution.statements().toLambdaBody());
	}

	@Nullable
	private TypeHint reflectionHints(CodeContribution contribution, Class<?> type) {
		return contribution.runtimeHints().reflection().getTypeHint(type);
	}

	private Consumer<TypeHint> hasSingleQueryConstructor(Constructor<?> constructor) {
		return typeHint -> assertThat(typeHint.constructors()).singleElement()
				.satisfies(match(constructor, "<init>", ExecutableMode.INTROSPECT));
	}

	private Consumer<TypeHint> hasSingleQueryMethod(Method method) {
		return typeHint -> assertThat(typeHint.methods()).singleElement()
				.satisfies(match(method, method.getName(), ExecutableMode.INTROSPECT));
	}

	private Consumer<ExecutableHint> match(Executable executable, String name, ExecutableMode... modes) {
		return hint -> {
			assertThat(hint.getName()).isEqualTo(name);
			assertThat(hint.getParameterTypes()).hasSameSizeAs(executable.getParameterTypes());
			for (int i = 0; i < hint.getParameterTypes().size(); i++) {
				assertThat(hint.getParameterTypes().get(i))
						.isEqualTo(TypeReference.of(executable.getParameterTypes()[i]));
			}
			assertThat(hint.getModes()).containsOnly(modes);
		};
	}

	private CodeContribution generate(Executable executable,
			BeanInstantiationContribution... beanInstantiationContributions) {
		DefaultBeanInstantiationGenerator generator = new DefaultBeanInstantiationGenerator(executable,
				Arrays.asList(beanInstantiationContributions));
		return generator.generateBeanInstantiation(new RuntimeHints());
	}

	private static Method method(Class<?> type, String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(type, methodName, parameterTypes);
		assertThat(method).isNotNull();
		return method;
	}

}
