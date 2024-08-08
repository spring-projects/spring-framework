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

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumer;

import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.aot.BeanInstanceSupplierTests.Enclosing.InnerSingleArgConstructor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.SimpleInstantiationStrategy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.ThrowingBiFunction;
import org.springframework.util.function.ThrowingFunction;
import org.springframework.util.function.ThrowingSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BeanInstanceSupplier}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class BeanInstanceSupplierTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void forConstructorWhenParameterTypesIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> BeanInstanceSupplier
						.forConstructor((Class<?>[]) null))
				.withMessage("'parameterTypes' must not be null");
	}

	@Test
	void forConstructorWhenParameterTypesContainsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> BeanInstanceSupplier
						.forConstructor(String.class, null))
				.withMessage("'parameterTypes' must not contain null elements");
	}

	@Test
	void forConstructorWhenNotFoundThrowsException() {
		BeanInstanceSupplier<InputStream> resolver = BeanInstanceSupplier
				.forConstructor(InputStream.class);
		Source source = new Source(SingleArgConstructor.class, resolver);
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.get(registerBean)).withMessage(
						"Constructor with parameter types [java.io.InputStream] cannot be found on "
								+ SingleArgConstructor.class.getName());
	}

	@Test
	void forConstructorReturnsNullFactoryMethod() {
		BeanInstanceSupplier<Object> resolver = BeanInstanceSupplier.forConstructor(String.class);
		assertThat(resolver.getFactoryMethod()).isNull();
	}

	@Test
	void forFactoryMethodWhenDeclaringClassIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> BeanInstanceSupplier
						.forFactoryMethod(null, "test"))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void forFactoryMethodWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> BeanInstanceSupplier
						.forFactoryMethod(SingleArgFactory.class, ""))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void forFactoryMethodWhenParameterTypesIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> BeanInstanceSupplier.forFactoryMethod(
								SingleArgFactory.class, "single", (Class<?>[]) null))
				.withMessage("'parameterTypes' must not be null");
	}

	@Test
	void forFactoryMethodWhenParameterTypesContainsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> BeanInstanceSupplier.forFactoryMethod(
								SingleArgFactory.class, "single", String.class, null))
				.withMessage("'parameterTypes' must not contain null elements");
	}

	@Test
	void forFactoryMethodWhenNotFoundThrowsException() {
		BeanInstanceSupplier<InputStream> resolver = BeanInstanceSupplier
				.forFactoryMethod(SingleArgFactory.class, "single", InputStream.class);
		Source source = new Source(String.class, resolver);
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.get(registerBean)).withMessage(
						"Factory method 'single' with parameter types [java.io.InputStream] declared on class "
								+ SingleArgFactory.class.getName() + " cannot be found");
	}

	@Test
	void forFactoryMethodReturnsFactoryMethod() {
		BeanInstanceSupplier<String> resolver = BeanInstanceSupplier
				.forFactoryMethod(SingleArgFactory.class, "single", String.class);
		Method factoryMethod = ReflectionUtils.findMethod(SingleArgFactory.class, "single", String.class);
		assertThat(factoryMethod).isNotNull();
		assertThat(resolver.getFactoryMethod()).isEqualTo(factoryMethod);
	}

	@Test
	void withGeneratorWhenBiFunctionIsNullThrowsException() {
		BeanInstanceSupplier<Object> resolver = BeanInstanceSupplier
				.forConstructor();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.withGenerator(
						(ThrowingBiFunction<RegisteredBean, AutowiredArguments, Object>) null))
				.withMessage("'generator' must not be null");
	}

	@Test
	void withGeneratorWhenFunctionIsNullThrowsException() {
		BeanInstanceSupplier<Object> resolver = BeanInstanceSupplier
				.forConstructor();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.withGenerator(
						(ThrowingFunction<RegisteredBean, Object>) null))
				.withMessage("'generator' must not be null");
	}

	@Test
	@Deprecated
	@SuppressWarnings("removal")
	void withGeneratorWhenSupplierIsNullThrowsException() {
		BeanInstanceSupplier<Object> resolver = BeanInstanceSupplier
				.forConstructor();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.withGenerator(
						(ThrowingSupplier<Object>) null))
				.withMessage("'generator' must not be null");
	}

	@Test
	void getWithConstructorDoesNotSetResolvedFactoryMethod() throws Exception {
		BeanInstanceSupplier<SingleArgConstructor> resolver = BeanInstanceSupplier
				.forConstructor(String.class);
		this.beanFactory.registerSingleton("one", "1");
		Source source = new Source(SingleArgConstructor.class, resolver);
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		assertThat(registerBean.getMergedBeanDefinition().getResolvedFactoryMethod()).isNull();
		source.getResolver().get(registerBean);
		assertThat(registerBean.getMergedBeanDefinition().getResolvedFactoryMethod()).isNull();
	}

	@Test
	void getWithFactoryMethodSetsResolvedFactoryMethod() {
		Method factoryMethod = ReflectionUtils.findMethod(SingleArgFactory.class, "single", String.class);
		assertThat(factoryMethod).isNotNull();
		BeanInstanceSupplier<String> resolver = BeanInstanceSupplier
				.forFactoryMethod(SingleArgFactory.class, "single", String.class);
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		assertThat(beanDefinition.getResolvedFactoryMethod()).isNull();
		beanDefinition.setInstanceSupplier(resolver);
		assertThat(beanDefinition.getResolvedFactoryMethod()).isEqualTo(factoryMethod);
	}

	@Test
	void getWithGeneratorCallsBiFunction() throws Exception {
		BeanRegistrar registrar = new BeanRegistrar(SingleArgConstructor.class);
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registerBean = registrar.registerBean(this.beanFactory);
		List<Object> result = new ArrayList<>();
		BeanInstanceSupplier<Object> resolver = BeanInstanceSupplier
				.forConstructor(String.class)
				.withGenerator((registeredBean, args) -> result.add(args));
		resolver.get(registerBean);
		assertThat(result).hasSize(1);
		assertThat(((AutowiredArguments) result.get(0)).toArray()).containsExactly("1");
	}

	@Test
	void getWithGeneratorCallsFunction() throws Exception {
		BeanRegistrar registrar = new BeanRegistrar(SingleArgConstructor.class);
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registerBean = registrar.registerBean(this.beanFactory);
		BeanInstanceSupplier<String> resolver = BeanInstanceSupplier
				.<String>forConstructor(String.class)
				.withGenerator(registeredBean -> "1");
		assertThat(resolver.get(registerBean)).isInstanceOf(String.class).isEqualTo("1");
	}

	@Test
	@Deprecated
	@SuppressWarnings("removal")
	void getWithGeneratorCallsSupplier() throws Exception {
		BeanRegistrar registrar = new BeanRegistrar(SingleArgConstructor.class);
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registerBean = registrar.registerBean(this.beanFactory);
		BeanInstanceSupplier<String> resolver = BeanInstanceSupplier
				.<String>forConstructor(String.class)
				.withGenerator(() -> "1");
		assertThat(resolver.get(registerBean)).isInstanceOf(String.class).isEqualTo("1");
	}

	@Test
	void getWhenRegisteredBeanIsNullThrowsException() {
		BeanInstanceSupplier<Object> resolver = BeanInstanceSupplier
				.forConstructor(String.class);
		assertThatIllegalArgumentException().isThrownBy(() -> resolver.get((RegisteredBean) null))
				.withMessage("'registeredBean' must not be null");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void getWithNoGeneratorUsesReflection(Source source) throws Exception {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("testFactory", new SingleArgFactory());
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object instance = source.getResolver().get(registerBean);
		if (instance instanceof SingleArgConstructor singleArgConstructor) {
			instance = singleArgConstructor.getString();
		}
		assertThat(instance).isEqualTo("1");
	}

	@ParameterizedResolverTest(Sources.INNER_CLASS_SINGLE_ARG)
	void getNestedWithNoGeneratorUsesReflection(Source source) throws Exception {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("testFactory",
				new Enclosing().new InnerSingleArgFactory());
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object instance = source.getResolver().get(registerBean);
		if (instance instanceof InnerSingleArgConstructor innerSingleArgConstructor) {
			instance = innerSingleArgConstructor.getString();
		}
		assertThat(instance).isEqualTo("1");
	}

	@Test // gh-33180
	void getWithNestedInvocationRetainsFactoryMethod() throws Exception {
		AtomicReference<Method> testMethodReference = new AtomicReference<>();
		AtomicReference<Method> anotherMethodReference = new AtomicReference<>();

		BeanInstanceSupplier<Object> nestedInstanceSupplier = BeanInstanceSupplier
				.forFactoryMethod(AnotherTestStringFactory.class, "another")
				.withGenerator(registeredBean -> {
					anotherMethodReference.set(SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod());
					return "Another";
				});
		RegisteredBean nestedRegisteredBean = new Source(String.class, nestedInstanceSupplier).registerBean(this.beanFactory);
		BeanInstanceSupplier<Object> instanceSupplier = BeanInstanceSupplier
				.forFactoryMethod(TestStringFactory.class, "test")
				.withGenerator(registeredBean -> {
					Object nested = nestedInstanceSupplier.get(nestedRegisteredBean);
					testMethodReference.set(SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod());
					return "custom" + nested;
				});
		RegisteredBean registeredBean = new Source(String.class, instanceSupplier).registerBean(this.beanFactory);
		Object value = instanceSupplier.get(registeredBean);

		assertThat(value).isEqualTo("customAnother");
		assertThat(testMethodReference.get()).isEqualTo(instanceSupplier.getFactoryMethod());
		assertThat(anotherMethodReference.get()).isEqualTo(nestedInstanceSupplier.getFactoryMethod());
	}

	@Test
	void resolveArgumentsWithNoArgConstructor() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				NoArgConstructor.class);
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "test");
		AutowiredArguments resolved = BeanInstanceSupplier
				.forConstructor().resolveArguments(registeredBean);
		assertThat(resolved.toArray()).isEmpty();
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveArgumentsWithSingleArgConstructor(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		assertThat(source.getResolver().resolveArguments(registeredBean).toArray())
				.containsExactly("1");
	}

	@ParameterizedResolverTest(Sources.INNER_CLASS_SINGLE_ARG)
	void resolveArgumentsWithNestedSingleArgConstructor(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		assertThat(source.getResolver().resolveArguments(registeredBean).toArray())
				.containsExactly("1");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveArgumentsWithRequiredDependencyNotPresentThrowsUnsatisfiedDependencyException(
			Source source) {
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> source.getResolver().resolveArguments(registeredBean))
				.satisfies(ex -> {
					assertThat(ex.getBeanName()).isEqualTo("testBean");
					assertThat(ex.getInjectionPoint()).isNotNull();
					assertThat(ex.getInjectionPoint().getMember())
							.isEqualTo(source.lookupExecutable(registeredBean));
				});
	}

	@Test
	void resolveArgumentsInInstanceSupplierWithSelfReferenceThrowsException() {
		// SingleArgFactory.single(...) expects a String to be injected
		// and our own bean is a String, so it's a valid candidate
		this.beanFactory.addBeanPostProcessor(new AutowiredAnnotationBeanPostProcessor());
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.setInstanceSupplier(InstanceSupplier.of(registeredBean -> {
			AutowiredArguments args = BeanInstanceSupplier
					.forFactoryMethod(SingleArgFactory.class, "single", String.class)
					.resolveArguments(registeredBean);
			return new SingleArgFactory().single(args.get(0));
		}));
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> this.beanFactory.getBean("test"));
	}

	@ParameterizedResolverTest(Sources.ARRAY_OF_BEANS)
	void resolveArgumentsWithArrayOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Object[]) arguments.get(0)).containsExactly("1", "2");
	}

	@ParameterizedResolverTest(Sources.ARRAY_OF_BEANS)
	void resolveArgumentsWithRequiredArrayOfBeansInjectEmptyArray(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Object[]) arguments.get(0)).isEmpty();
	}

	@ParameterizedResolverTest(Sources.LIST_OF_BEANS)
	void resolveArgumentsWithListOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isInstanceOf(List.class).asInstanceOf(LIST)
				.containsExactly("1", "2");
	}

	@ParameterizedResolverTest(Sources.LIST_OF_BEANS)
	void resolveArgumentsWithRequiredListOfBeansInjectEmptyList(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((List<?>) arguments.get(0)).isEmpty();
	}

	@ParameterizedResolverTest(Sources.SET_OF_BEANS)
	@SuppressWarnings("unchecked")
	void resolveArgumentsWithSetOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Set<String>) arguments.get(0)).containsExactly("1", "2");
	}

	@ParameterizedResolverTest(Sources.SET_OF_BEANS)
	void resolveArgumentsWithRequiredSetOfBeansInjectEmptySet(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Set<?>) arguments.get(0)).isEmpty();
	}

	@ParameterizedResolverTest(Sources.MAP_OF_BEANS)
	@SuppressWarnings("unchecked")
	void resolveArgumentsWithMapOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Map<String, String>) arguments.get(0))
				.containsExactly(entry("one", "1"), entry("two", "2"));
	}

	@ParameterizedResolverTest(Sources.MAP_OF_BEANS)
	void resolveArgumentsWithRequiredMapOfBeansInjectEmptySet(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Map<?, ?>) arguments.get(0)).isEmpty();
	}

	@ParameterizedResolverTest(Sources.MULTI_ARGS)
	void resolveArgumentsWithMultiArgsConstructor(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock();
		this.beanFactory.registerResolvableDependency(ResourceLoader.class,
				resourceLoader);
		this.beanFactory.registerSingleton("environment", environment);
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(3);
		assertThat(arguments.getObject(0)).isEqualTo(resourceLoader);
		assertThat(arguments.getObject(1)).isEqualTo(environment);
		assertThat(((ObjectProvider<?>) arguments.get(2)).getIfAvailable())
				.isEqualTo("1");
	}

	@ParameterizedResolverTest(Sources.MIXED_ARGS)
	void resolveArgumentsWithMixedArgsConstructorWithIndexedUserValue(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock();
		this.beanFactory.registerResolvableDependency(ResourceLoader.class,
				resourceLoader);
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> {
					beanDefinition
							.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
					beanDefinition.getConstructorArgumentValues()
							.addIndexedArgumentValue(1, "user-value");
				});
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(3);
		assertThat(arguments.getObject(0)).isEqualTo(resourceLoader);
		assertThat(arguments.getObject(1)).isEqualTo("user-value");
		assertThat(arguments.getObject(2)).isEqualTo(environment);
	}

	@ParameterizedResolverTest(Sources.MIXED_ARGS)
	void resolveArgumentsWithMixedArgsConstructorWithGenericUserValue(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock();
		this.beanFactory.registerResolvableDependency(ResourceLoader.class,
				resourceLoader);
		this.beanFactory.registerSingleton("environment", environment);
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> {
					beanDefinition
							.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
					beanDefinition.getConstructorArgumentValues()
							.addGenericArgumentValue("user-value");
				});
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(3);
		assertThat(arguments.getObject(0)).isEqualTo(resourceLoader);
		assertThat(arguments.getObject(1)).isEqualTo("user-value");
		assertThat(arguments.getObject(2)).isEqualTo(environment);
	}

	@ParameterizedResolverTest(Sources.MIXED_ARGS)
	void resolveArgumentsWithMixedArgsConstructorAndIndexedUserBeanReference(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock();
		this.beanFactory.registerResolvableDependency(ResourceLoader.class,
				resourceLoader);
		this.beanFactory.registerSingleton("environment", environment);
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> {
					beanDefinition
							.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
					beanDefinition.getConstructorArgumentValues()
							.addIndexedArgumentValue(1, new RuntimeBeanReference("two"));
				});
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(3);
		assertThat(arguments.getObject(0)).isEqualTo(resourceLoader);
		assertThat(arguments.getObject(1)).isEqualTo("2");
		assertThat(arguments.getObject(2)).isEqualTo(environment);
	}

	@ParameterizedResolverTest(Sources.MIXED_ARGS)
	void resolveArgumentsWithMixedArgsConstructorAndGenericUserBeanReference(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock();
		this.beanFactory.registerResolvableDependency(ResourceLoader.class,
				resourceLoader);
		this.beanFactory.registerSingleton("environment", environment);
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> {
					beanDefinition
							.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
					beanDefinition.getConstructorArgumentValues()
							.addGenericArgumentValue(new RuntimeBeanReference("two"));
				});
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(3);
		assertThat(arguments.getObject(0)).isEqualTo(resourceLoader);
		assertThat(arguments.getObject(1)).isEqualTo("2");
		assertThat(arguments.getObject(2)).isEqualTo(environment);
	}

	@Test
	void resolveIndexedArgumentsWithUserValueWithTypeConversionRequired() {
		Source source = new Source(CharDependency.class,
				BeanInstanceSupplier.forConstructor(char.class));
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> {
					beanDefinition
							.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
					beanDefinition.getConstructorArgumentValues()
							.addIndexedArgumentValue(0, "\\");
				});
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isInstanceOf(Character.class).isEqualTo('\\');
	}

	@Test
	void resolveGenericArgumentsWithUserValueWithTypeConversionRequired() {
		Source source = new Source(CharDependency.class,
				BeanInstanceSupplier.forConstructor(char.class));
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> {
					beanDefinition
							.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
					beanDefinition.getConstructorArgumentValues()
							.addGenericArgumentValue("\\", char.class.getName());
				});
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isInstanceOf(Character.class).isEqualTo('\\');
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveIndexedArgumentsWithUserValueWithBeanReference(Source source) {
		this.beanFactory.registerSingleton("stringBean", "string");
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> beanDefinition.getConstructorArgumentValues()
						.addIndexedArgumentValue(0,
								new RuntimeBeanReference("stringBean")));
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isEqualTo("string");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveGenericArgumentsWithUserValueWithBeanReference(Source source) {
		this.beanFactory.registerSingleton("stringBean", "string");
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> beanDefinition.getConstructorArgumentValues()
						.addGenericArgumentValue(new RuntimeBeanReference("stringBean")));
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isEqualTo("string");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveIndexedArgumentsWithUserValueWithBeanDefinition(Source source) {
		AbstractBeanDefinition userValue = BeanDefinitionBuilder
				.rootBeanDefinition(String.class, () -> "string").getBeanDefinition();
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> beanDefinition.getConstructorArgumentValues()
						.addIndexedArgumentValue(0, userValue));
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isEqualTo("string");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveGenericArgumentsWithUserValueWithBeanDefinition(Source source) {
		AbstractBeanDefinition userValue = BeanDefinitionBuilder
				.rootBeanDefinition(String.class, () -> "string").getBeanDefinition();
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> beanDefinition.getConstructorArgumentValues()
						.addGenericArgumentValue(userValue));
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isEqualTo("string");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveIndexedArgumentsWithUserValueThatIsAlreadyResolved(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		BeanDefinition mergedBeanDefinition = this.beanFactory
				.getMergedBeanDefinition("testBean");
		ValueHolder valueHolder = new ValueHolder("a");
		valueHolder.setConvertedValue("this is an a");
		mergedBeanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				valueHolder);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isEqualTo("this is an a");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveGenericArgumentsWithUserValueThatIsAlreadyResolved(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		BeanDefinition mergedBeanDefinition = this.beanFactory
				.getMergedBeanDefinition("testBean");
		ValueHolder valueHolder = new ValueHolder("a");
		valueHolder.setConvertedValue("this is an a");
		mergedBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
		AutowiredArguments arguments = source.getResolver().resolveArguments(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isEqualTo("this is an a");
	}

	@Test
	void resolveArgumentsWhenUsingShortcutsInjectsDirectly() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory() {

			@Override
			protected Map<String, Object> findAutowireCandidates(String beanName,
					Class<?> requiredType, DependencyDescriptor descriptor) {
				throw new AssertionError("Should be shortcut");
			}

		};
		BeanInstanceSupplier<Object> resolver = BeanInstanceSupplier
				.forConstructor(String.class);
		Source source = new Source(String.class, resolver);
		beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(beanFactory);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resolver.resolveArguments(registeredBean));
		assertThat(resolver.withShortcuts("one").resolveArguments(registeredBean).toArray())
				.containsExactly("1");
	}

	@Test
	void resolveArgumentsRegistersDependantBeans() {
		BeanInstanceSupplier<Object> resolver = BeanInstanceSupplier
				.forConstructor(String.class);
		Source source = new Source(SingleArgConstructor.class, resolver);
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		resolver.resolveArguments(registeredBean);
		assertThat(this.beanFactory.getDependentBeans("one")).containsExactly("testBean");
	}

	/**
	 * Parameterized test backed by a {@link Sources}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@ArgumentsSource(SourcesArguments.class)
	@interface ParameterizedResolverTest {

		Sources value();

	}

	/**
	 * {@link ArgumentsProvider} delegating to the {@link Sources}.
	 */
	static class SourcesArguments
			implements ArgumentsProvider, AnnotationConsumer<ParameterizedResolverTest> {

		private Sources source;

		@Override
		public void accept(ParameterizedResolverTest annotation) {
			this.source = annotation.value();
		}

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			return this.source.provideArguments(context);
		}

	}

	/**
	 * Sources for parameterized tests.
	 */
	enum Sources {

		SINGLE_ARG {
			@Override
			protected void setup() {
				add(SingleArgConstructor.class, BeanInstanceSupplier
						.forConstructor(String.class));
				add(String.class,
						BeanInstanceSupplier.forFactoryMethod(
								SingleArgFactory.class, "single", String.class));
			}

		},

		INNER_CLASS_SINGLE_ARG {
			@Override
			protected void setup() {
				add(Enclosing.InnerSingleArgConstructor.class,
						BeanInstanceSupplier
								.forConstructor(String.class));
				add(String.class,
						BeanInstanceSupplier.forFactoryMethod(
								Enclosing.InnerSingleArgFactory.class, "single",
								String.class));
			}

		},

		ARRAY_OF_BEANS {
			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						BeanInstanceSupplier
								.forConstructor(String[].class));
				add(String.class,
						BeanInstanceSupplier.forFactoryMethod(
								BeansCollectionFactory.class, "array", String[].class));
			}

		},

		LIST_OF_BEANS {
			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						BeanInstanceSupplier
								.forConstructor(List.class));
				add(String.class,
						BeanInstanceSupplier.forFactoryMethod(
								BeansCollectionFactory.class, "list", List.class));
			}

		},

		SET_OF_BEANS {
			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						BeanInstanceSupplier
								.forConstructor(Set.class));
				add(String.class,
						BeanInstanceSupplier.forFactoryMethod(
								BeansCollectionFactory.class, "set", Set.class));
			}

		},

		MAP_OF_BEANS {
			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						BeanInstanceSupplier
								.forConstructor(Map.class));
				add(String.class,
						BeanInstanceSupplier.forFactoryMethod(
								BeansCollectionFactory.class, "map", Map.class));
			}

		},

		MULTI_ARGS {
			@Override
			protected void setup() {
				add(MultiArgsConstructor.class,
						BeanInstanceSupplier.forConstructor(
								ResourceLoader.class, Environment.class,
								ObjectProvider.class));
				add(String.class,
						BeanInstanceSupplier.forFactoryMethod(
								MultiArgsFactory.class, "multiArgs", ResourceLoader.class,
								Environment.class, ObjectProvider.class));
			}

		},

		MIXED_ARGS {
			@Override
			protected void setup() {
				add(MixedArgsConstructor.class,
						BeanInstanceSupplier.forConstructor(
								ResourceLoader.class, String.class, Environment.class));
				add(String.class,
						BeanInstanceSupplier.forFactoryMethod(
								MixedArgsFactory.class, "mixedArgs", ResourceLoader.class,
								String.class, Environment.class));
			}

		};

		private final List<Arguments> arguments;

		Sources() {
			this.arguments = new ArrayList<>();
			setup();
		}

		protected abstract void setup();

		protected final void add(Class<?> beanClass,
				BeanInstanceSupplier<?> resolver) {
			this.arguments.add(Arguments.of(new Source(beanClass, resolver)));
		}

		final Stream<Arguments> provideArguments(ExtensionContext context) {
			return this.arguments.stream();
		}

	}

	static class BeanRegistrar {

		final Class<?> beanClass;

		public BeanRegistrar(Class<?> beanClass) {
			this.beanClass = beanClass;
		}

		RegisteredBean registerBean(DefaultListableBeanFactory beanFactory) {
			return registerBean(beanFactory, beanDefinition -> {
			});
		}

		RegisteredBean registerBean(DefaultListableBeanFactory beanFactory,
				Consumer<RootBeanDefinition> beanDefinitionCustomizer) {
			String beanName = "testBean";
			RootBeanDefinition beanDefinition = new RootBeanDefinition(this.beanClass);
			beanDefinition.setInstanceSupplier(() -> {
				throw new BeanCurrentlyInCreationException(beanName);
			});
			beanDefinitionCustomizer.accept(beanDefinition);
			beanFactory.registerBeanDefinition(beanName, beanDefinition);
			return RegisteredBean.of(beanFactory, beanName);
		}
	}

	static class Source extends BeanRegistrar {

		private final BeanInstanceSupplier<?> resolver;

		public Source(Class<?> beanClass,
				BeanInstanceSupplier<?> resolver) {
			super(beanClass);
			this.resolver = resolver;
		}

		BeanInstanceSupplier<?> getResolver() {
			return this.resolver;
		}

		Executable lookupExecutable(RegisteredBean registeredBean) {
			return this.resolver.getLookup().get(registeredBean);
		}

		@Override
		public String toString() {
			return this.resolver.getLookup() + " with bean class "
					+ ClassUtils.getShortName(this.beanClass);
		}

	}

	static class NoArgConstructor {

	}

	static class SingleArgConstructor {

		private final String string;

		SingleArgConstructor(String string) {
			this.string = string;
		}

		String getString() {
			return this.string;
		}

	}

	static class SingleArgFactory {

		String single(String s) {
			return s;
		}

	}

	static class Enclosing {

		class InnerSingleArgConstructor {

			private final String string;

			InnerSingleArgConstructor(String string) {
				this.string = string;
			}

			String getString() {
				return this.string;
			}

		}

		class InnerSingleArgFactory {

			String single(String s) {
				return s;
			}

		}

	}

	static class BeansCollectionConstructor {

		public BeansCollectionConstructor(String[] beans) {

		}

		public BeansCollectionConstructor(List<String> beans) {

		}

		public BeansCollectionConstructor(Set<String> beans) {

		}

		public BeansCollectionConstructor(Map<String, String> beans) {

		}

	}

	static class BeansCollectionFactory {

		public String array(String[] beans) {
			return "test";
		}

		public String list(List<String> beans) {
			return "test";
		}

		public String set(Set<String> beans) {
			return "test";
		}

		public String map(Map<String, String> beans) {
			return "test";
		}

	}

	static class MultiArgsConstructor {

		public MultiArgsConstructor(ResourceLoader resourceLoader,
				Environment environment, ObjectProvider<String> provider) {
		}
	}

	static class MultiArgsFactory {

		String multiArgs(ResourceLoader resourceLoader, Environment environment,
				ObjectProvider<String> provider) {
			return "test";
		}
	}

	static class MixedArgsConstructor {

		public MixedArgsConstructor(ResourceLoader resourceLoader, String test,
				Environment environment) {

		}

	}

	static class MixedArgsFactory {

		String mixedArgs(ResourceLoader resourceLoader, String test,
				Environment environment) {
			return "test";
		}

	}

	static class CharDependency {

		CharDependency(char escapeChar) {
		}

	}

	interface MethodOnInterface {

		default String test() {
			return "Test";
		}

	}

	static class MethodOnInterfaceImpl implements MethodOnInterface {

	}

	static class TestStringFactory {

		String test() {
			return "test";
		}
	}

	static class AnotherTestStringFactory {

		String another() {
			return "another";
		}
	}

}
