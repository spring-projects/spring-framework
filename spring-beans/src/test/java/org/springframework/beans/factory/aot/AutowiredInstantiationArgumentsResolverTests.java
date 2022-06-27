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

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.beans.factory.aot.AutowiredInstantiationArgumentsResolverTests.Enclosing.InnerSingleArgConstructor;
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
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutowiredInstantiationArgumentsResolver}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AutowiredInstantiationArgumentsResolverTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void forConstructorWhenParameterTypesIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredInstantiationArgumentsResolver
						.forConstructor((Class<?>[]) null))
				.withMessage("'parameterTypes' must not be null");
	}

	@Test
	void forConstructorWhenParameterTypesContainsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredInstantiationArgumentsResolver
						.forConstructor(String.class, null))
				.withMessage("'parameterTypes' must not contain null elements");
	}

	@Test
	void forConstructorWhenNotFoundThrowsException() {
		AutowiredInstantiationArgumentsResolver resolver = AutowiredInstantiationArgumentsResolver
				.forConstructor(InputStream.class);
		Source source = new Source(SingleArgConstructor.class, resolver);
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.resolve(registerBean)).withMessage(
						"Constructor with parameter types [java.io.InputStream] cannot be found on "
								+ SingleArgConstructor.class.getName());
	}

	@Test
	void forFactoryMethodWhenDeclaringClassIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredInstantiationArgumentsResolver
						.forFactoryMethod(null, "test"))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void forFactoryMethodWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> AutowiredInstantiationArgumentsResolver
						.forFactoryMethod(SingleArgFactory.class, ""))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void forFactoryMethodWhenParameterTypesIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								SingleArgFactory.class, "single", (Class<?>[]) null))
				.withMessage("'parameterTypes' must not be null");
	}

	@Test
	void forFactoryMethodWhenParameterTypesContainsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								SingleArgFactory.class, "single", String.class, null))
				.withMessage("'parameterTypes' must not contain null elements");
	}

	@Test
	void forFactoryMethodWhenNotFoundThrowsException() {
		AutowiredInstantiationArgumentsResolver resolver = AutowiredInstantiationArgumentsResolver
				.forFactoryMethod(SingleArgFactory.class, "single", InputStream.class);
		Source source = new Source(String.class, resolver);
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.resolve(registerBean)).withMessage(
						"Factory method 'single' with parameter types [java.io.InputStream] declared on class "
								+ SingleArgFactory.class.getName() + " cannot be found");
	}

	@Test
	void resolveWithActionWhenActionIsNullThrowsException() {
		AutowiredInstantiationArgumentsResolver resolver = AutowiredInstantiationArgumentsResolver
				.forConstructor();
		Source source = new Source(NoArgConstructor.class, resolver);
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.resolve(registerBean, null))
				.withMessage("'action' must not be null");
	}

	@Test
	void resolveWithActionCallsAction() {
		AutowiredInstantiationArgumentsResolver resolver = AutowiredInstantiationArgumentsResolver
				.forConstructor(String.class);
		Source source = new Source(SingleArgConstructor.class, resolver);
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		List<Object> result = new ArrayList<>();
		resolver.resolve(registerBean, result::add);
		assertThat(result).hasSize(1);
		assertThat(((AutowiredArguments) result.get(0)).toArray()).containsExactly("1");
	}

	@Test
	void resolveWhenRegisteredBeanIsNullThrowsException() {
		AutowiredInstantiationArgumentsResolver resolver = AutowiredInstantiationArgumentsResolver
				.forConstructor(String.class);
		assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolve(null))
				.withMessage("'registeredBean' must not be null");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveAndInstantiate(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("testFactory", new SingleArgFactory());
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object instance = source.getResolver().resolveAndInstantiate(registerBean);
		if (instance instanceof SingleArgConstructor singleArgConstructor) {
			instance = singleArgConstructor.getString();
		}
		assertThat(instance).isEqualTo("1");
	}

	@ParameterizedResolverTest(Sources.INNER_CLASS_SINGLE_ARG)
	void resolveAndInstantiateNested(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("testFactory",
				new Enclosing().new InnerSingleArgFactory());
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		Object instance = source.getResolver().resolveAndInstantiate(registerBean);
		if (instance instanceof InnerSingleArgConstructor innerSingleArgConstructor) {
			instance = innerSingleArgConstructor.getString();
		}
		assertThat(instance).isEqualTo("1");
	}

	@Test
	void resolveNoArgConstructor() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				NoArgConstructor.class);
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "test");
		AutowiredArguments resolved = AutowiredInstantiationArgumentsResolver
				.forConstructor().resolve(registeredBean);
		assertThat(resolved.toArray()).isEmpty();
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveSingleArgConstructor(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		assertThat(source.getResolver().resolve(registeredBean).toArray())
				.containsExactly("1");
	}

	@ParameterizedResolverTest(Sources.INNER_CLASS_SINGLE_ARG)
	void resolvedNestedSingleArgConstructor(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		assertThat(source.getResolver().resolve(registeredBean).toArray())
				.containsExactly("1");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveRequiredDependencyNotPresentThrowsUnsatisfiedDependencyException(
			Source source) {
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> source.getResolver().resolve(registeredBean))
				.satisfies(ex -> {
					assertThat(ex.getBeanName()).isEqualTo("testBean");
					assertThat(ex.getInjectionPoint()).isNotNull();
					assertThat(ex.getInjectionPoint().getMember())
							.isEqualTo(source.lookupExecutable(registeredBean));
				});
	}

	@Test
	void resolveInInstanceSupplierWithSelfReferenceThrowsException() {
		// SingleArgFactory.single(...) expects a String to be injected
		// and our own bean is a String so it's a valid candidate
		this.beanFactory.addBeanPostProcessor(new AutowiredAnnotationBeanPostProcessor());
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		beanDefinition.setInstanceSupplier(InstanceSupplier.of(registeredBean -> {
			AutowiredArguments args = AutowiredInstantiationArgumentsResolver
					.forFactoryMethod(SingleArgFactory.class, "single", String.class)
					.resolve(registeredBean);
			return new SingleArgFactory().single((String) args.get(0));
		}));
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> this.beanFactory.getBean("test"));
	}

	@ParameterizedResolverTest(Sources.ARRAY_OF_BEANS)
	void resolveArrayOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Object[]) arguments.get(0)).containsExactly("1", "2");
	}

	@ParameterizedResolverTest(Sources.ARRAY_OF_BEANS)
	void resolveRequiredArrayOfBeansInjectEmptyArray(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Object[]) arguments.get(0)).isEmpty();
	}

	@ParameterizedResolverTest(Sources.LIST_OF_BEANS)
	void resolveListOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isInstanceOf(List.class).asList()
				.containsExactly("1", "2");
	}

	@ParameterizedResolverTest(Sources.LIST_OF_BEANS)
	void resolveRequiredListOfBeansInjectEmptyList(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((List<?>) arguments.get(0)).isEmpty();
	}

	@ParameterizedResolverTest(Sources.SET_OF_BEANS)
	@SuppressWarnings("unchecked")
	void resolveSetOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Set<String>) arguments.get(0)).containsExactly("1", "2");
	}

	@ParameterizedResolverTest(Sources.SET_OF_BEANS)
	void resolveRequiredSetOfBeansInjectEmptySet(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Set<?>) arguments.get(0)).isEmpty();
	}

	@ParameterizedResolverTest(Sources.MAP_OF_BEANS)
	@SuppressWarnings("unchecked")
	void resolveMapOfBeans(Source source) {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Map<String, String>) arguments.get(0))
				.containsExactly(entry("one", "1"), entry("two", "2"));
	}

	@ParameterizedResolverTest(Sources.MAP_OF_BEANS)
	void resolveRequiredMapOfBeansInjectEmptySet(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat((Map<?, ?>) arguments.get(0)).isEmpty();
	}

	@ParameterizedResolverTest(Sources.MULTI_ARGS)
	void resolveMultiArgsConstructor(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock(Environment.class);
		this.beanFactory.registerResolvableDependency(ResourceLoader.class,
				resourceLoader);
		this.beanFactory.registerSingleton("environment", environment);
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(3);
		assertThat(arguments.getObject(0)).isEqualTo(resourceLoader);
		assertThat(arguments.getObject(1)).isEqualTo(environment);
		assertThat(((ObjectProvider<?>) arguments.get(2)).getIfAvailable())
				.isEqualTo("1");
	}

	@ParameterizedResolverTest(Sources.MIXED_ARGS)
	void resolveMixedArgsConstructorWithUserValue(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock(Environment.class);
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
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(3);
		assertThat(arguments.getObject(0)).isEqualTo(resourceLoader);
		assertThat(arguments.getObject(1)).isEqualTo("user-value");
		assertThat(arguments.getObject(2)).isEqualTo(environment);
	}

	@ParameterizedResolverTest(Sources.MIXED_ARGS)
	void resolveMixedArgsConstructorWithUserBeanReference(Source source) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Environment environment = mock(Environment.class);
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
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(3);
		assertThat(arguments.getObject(0)).isEqualTo(resourceLoader);
		assertThat(arguments.getObject(1)).isEqualTo("2");
		assertThat(arguments.getObject(2)).isEqualTo(environment);
	}

	@Test
	void resolveUserValueWithTypeConversionRequired() {
		Source source = new Source(CharDependency.class,
				AutowiredInstantiationArgumentsResolver.forConstructor(char.class));
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> {
					beanDefinition
							.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
					beanDefinition.getConstructorArgumentValues()
							.addIndexedArgumentValue(0, "\\");
				});
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isInstanceOf(Character.class).isEqualTo('\\');
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveUserValueWithBeanReference(Source source) {
		this.beanFactory.registerSingleton("stringBean", "string");
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> beanDefinition.getConstructorArgumentValues()
						.addIndexedArgumentValue(0,
								new RuntimeBeanReference("stringBean")));
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isEqualTo("string");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveUserValueWithBeanDefinition(Source source) {
		AbstractBeanDefinition userValue = BeanDefinitionBuilder
				.rootBeanDefinition(String.class, () -> "string").getBeanDefinition();
		RegisteredBean registerBean = source.registerBean(this.beanFactory,
				beanDefinition -> beanDefinition.getConstructorArgumentValues()
						.addIndexedArgumentValue(0, userValue));
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isEqualTo("string");
	}

	@ParameterizedResolverTest(Sources.SINGLE_ARG)
	void resolveUserValueThatIsAlreadyResolved(Source source) {
		RegisteredBean registerBean = source.registerBean(this.beanFactory);
		BeanDefinition mergedBeanDefinition = this.beanFactory
				.getMergedBeanDefinition("testBean");
		ValueHolder valueHolder = new ValueHolder('a');
		valueHolder.setConvertedValue("this is an a");
		mergedBeanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				valueHolder);
		AutowiredArguments arguments = source.getResolver().resolve(registerBean);
		assertThat(arguments.toArray()).hasSize(1);
		assertThat(arguments.getObject(0)).isEqualTo("this is an a");
	}

	@Test
	void resolveWhenUsingShortcutsInjectsDirectly() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory() {

			@Override
			protected Map<String, Object> findAutowireCandidates(String beanName,
					Class<?> requiredType, DependencyDescriptor descriptor) {
				throw new AssertionError("Should be shortcut");
			}

		};
		AutowiredInstantiationArgumentsResolver resolver = AutowiredInstantiationArgumentsResolver
				.forConstructor(String.class);
		Source source = new Source(String.class, resolver);
		beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(beanFactory);
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> resolver.resolve(registeredBean));
		assertThat(resolver.withShortcuts("one").resolve(registeredBean).toArray())
				.containsExactly("1");
	}

	@Test
	void resolveRegistersDependantBeans() {
		AutowiredInstantiationArgumentsResolver resolver = AutowiredInstantiationArgumentsResolver
				.forConstructor(String.class);
		Source source = new Source(SingleArgConstructor.class, resolver);
		beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = source.registerBean(this.beanFactory);
		resolver.resolve(registeredBean);
		assertThat(this.beanFactory.getDependentBeans("one")).containsExactly("testBean");
	}

	/**
	 * Parameterized {@link Using} test backed by a {@link Sources}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@ArgumentsSource(SourcesArguments.class)
	static @interface ParameterizedResolverTest {

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
		public Stream<? extends Arguments> provideArguments(ExtensionContext context)
				throws Exception {
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
				add(SingleArgConstructor.class, AutowiredInstantiationArgumentsResolver
						.forConstructor(String.class));
				add(String.class,
						AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								SingleArgFactory.class, "single", String.class));
			}

		},

		INNER_CLASS_SINGLE_ARG {

			@Override
			protected void setup() {
				add(Enclosing.InnerSingleArgConstructor.class,
						AutowiredInstantiationArgumentsResolver
								.forConstructor(String.class));
				add(String.class,
						AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								Enclosing.InnerSingleArgFactory.class, "single",
								String.class));
			}

		},

		ARRAY_OF_BEANS {

			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						AutowiredInstantiationArgumentsResolver
								.forConstructor(String[].class));
				add(String.class,
						AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								BeansCollectionFactory.class, "array", String[].class));
			}

		},

		LIST_OF_BEANS {

			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						AutowiredInstantiationArgumentsResolver
								.forConstructor(List.class));
				add(String.class,
						AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								BeansCollectionFactory.class, "list", List.class));
			}

		},

		SET_OF_BEANS {

			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						AutowiredInstantiationArgumentsResolver
								.forConstructor(Set.class));
				add(String.class,
						AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								BeansCollectionFactory.class, "set", Set.class));
			}

		},

		MAP_OF_BEANS {

			@Override
			protected void setup() {
				add(BeansCollectionConstructor.class,
						AutowiredInstantiationArgumentsResolver
								.forConstructor(Map.class));
				add(String.class,
						AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								BeansCollectionFactory.class, "map", Map.class));
			}

		},

		MULTI_ARGS {

			@Override
			protected void setup() {
				add(MultiArgsConstructor.class,
						AutowiredInstantiationArgumentsResolver.forConstructor(
								ResourceLoader.class, Environment.class,
								ObjectProvider.class));
				add(String.class,
						AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								MultiArgsFactory.class, "multiArgs", ResourceLoader.class,
								Environment.class, ObjectProvider.class));
			}

		},

		MIXED_ARGS {

			@Override
			protected void setup() {
				add(MixedArgsConstructor.class,
						AutowiredInstantiationArgumentsResolver.forConstructor(
								ResourceLoader.class, String.class, Environment.class));
				add(String.class,
						AutowiredInstantiationArgumentsResolver.forFactoryMethod(
								MixedArgsFactory.class, "mixedArgs", ResourceLoader.class,
								String.class, Environment.class));
			}

		};

		private final List<Arguments> arguments;

		private Sources() {
			this.arguments = new ArrayList<>();
			setup();
		}

		protected abstract void setup();

		protected final void add(Class<?> beanClass,
				AutowiredInstantiationArgumentsResolver resolver) {
			this.arguments.add(Arguments.of(new Source(beanClass, resolver)));
		}

		final Stream<Arguments> provideArguments(ExtensionContext context) {
			return this.arguments.stream();
		}

	}

	static class Source {

		private final Class<?> beanClass;

		private final AutowiredInstantiationArgumentsResolver resolver;

		public Source(Class<?> beanClass,
				AutowiredInstantiationArgumentsResolver resolver) {
			this.beanClass = beanClass;
			this.resolver = resolver;
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

		AutowiredInstantiationArgumentsResolver getResolver() {
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
			return string;
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

	static interface MethodOnInterface {

		default String test() {
			return "Test";
		}

	}

	static class MethodOnInterfaceImpl implements MethodOnInterface {

	}

}
