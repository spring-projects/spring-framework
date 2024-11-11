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

package org.springframework.beans.factory.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import org.springframework.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConstructorResolver} focused on AOT constructor and factory
 * method resolution.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ConstructorResolverAotTests {

	@Test
	void detectBeanInstanceExecutableWithBeanClassAndFactoryMethodName() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testBean", "test");
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
				.addConstructorArgReference("testBean").getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				ReflectionUtils.findMethod(SampleFactory.class, "create", String.class));
	}

	@Test
	void detectBeanInstanceExecutableWithBeanClassNameAndFactoryMethodName() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testBean", "test");
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(SampleFactory.class.getName())
				.setFactoryMethod("create").addConstructorArgReference("testBean")
				.getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				ReflectionUtils.findMethod(SampleFactory.class, "create", String.class));
	}

	@Test
	void beanDefinitionWithFactoryMethodNameAndAssignableIndexedConstructorArgs() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testNumber", 1L);
		beanFactory.registerSingleton("testBean", "test");
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
				.addConstructorArgReference("testNumber")
				.addConstructorArgReference("testBean").getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
				.findMethod(SampleFactory.class, "create", Number.class, String.class));
	}

	@Test
	void beanDefinitionWithFactoryMethodNameAndAssignableGenericConstructorArgs() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
				.getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("test");
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(1L);
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
				.findMethod(SampleFactory.class, "create", Number.class, String.class));
	}

	@Test
	void beanDefinitionWithFactoryMethodNameAndAssignableTypeStringValues() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
				.getBeanDefinition();
		beanDefinition.getConstructorArgumentValues()
				.addGenericArgumentValue(new TypedStringValue("test"));
		beanDefinition.getConstructorArgumentValues()
				.addGenericArgumentValue(new TypedStringValue("1", Integer.class));
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
				.findMethod(SampleFactory.class, "create", Number.class, String.class));
	}

	@Test
	void beanDefinitionWithFactoryMethodNameAndMatchingMethodNames() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(DummySampleFactory.class).setFactoryMethod("of")
				.addConstructorArgValue(42).getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
				.findMethod(DummySampleFactory.class, "of", Integer.class));
	}

	@Test
	void beanDefinitionWithFactoryMethodNameAndOverriddenMethod() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ExtendedSampleFactory.class));
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(String.class).setFactoryMethodOnBean("resolve", "config")
				.addConstructorArgValue("test").getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
				.findMethod(ExtendedSampleFactory.class, "resolve", String.class));
	}

	@Test
	void detectBeanInstanceExecutableWithBeanClassAndFactoryMethodNameIgnoreTargetType() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testBean", "test");
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
				.addConstructorArgReference("testBean").getBeanDefinition();
		beanDefinition.setTargetType(String.class);
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				ReflectionUtils.findMethod(SampleFactory.class, "create", String.class));
	}

	@Test
	void beanDefinitionWithIndexedConstructorArgsForMultipleConstructors() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testNumber", 1L);
		beanFactory.registerSingleton("testBean", "test");
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(SampleBeanWithConstructors.class)
				.addConstructorArgReference("testNumber")
				.addConstructorArgReference("testBean").getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(SampleBeanWithConstructors.class
				.getDeclaredConstructor(Number.class, String.class));
	}

	@Test
	void beanDefinitionWithGenericConstructorArgsForMultipleConstructors() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testNumber", 1L);
		beanFactory.registerSingleton("testBean", "test");
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(SampleBeanWithConstructors.class)
				.getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("test");
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(1L);
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(SampleBeanWithConstructors.class
				.getDeclaredConstructor(Number.class, String.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingIndexedValue() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(42).getBeanDefinition();
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingGenericValue() throws NoSuchMethodException {
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSample.class)
				.getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(42);
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingArrayFromIndexedValue() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorArraySample.class)
				.addConstructorArgValue(42).getBeanDefinition();
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(MultiConstructorArraySample.class
				.getDeclaredConstructor(Integer[].class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingArrayFromGenericValue() throws NoSuchMethodException {
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorArraySample.class)
				.getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(42);
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(MultiConstructorArraySample.class
				.getDeclaredConstructor(Integer[].class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingListFromIndexedValue() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorListSample.class)
				.addConstructorArgValue(42).getBeanDefinition();
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				MultiConstructorListSample.class.getDeclaredConstructor(List.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingListFromGenericValue() throws NoSuchMethodException {
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorListSample.class)
				.getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(42);
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				MultiConstructorListSample.class.getDeclaredConstructor(List.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingIndexedValueAsInnerBean() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(
						BeanDefinitionBuilder.rootBeanDefinition(Integer.class, "valueOf")
								.addConstructorArgValue("42").getBeanDefinition())
				.getBeanDefinition();
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingGenericValueAsInnerBean() throws NoSuchMethodException {
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSample.class)
				.getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
						BeanDefinitionBuilder.rootBeanDefinition(Integer.class, "valueOf")
								.addConstructorArgValue("42").getBeanDefinition());
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingIndexedValueAsInnerBeanFactory() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(BeanDefinitionBuilder
						.rootBeanDefinition(IntegerFactoryBean.class).getBeanDefinition())
				.getBeanDefinition();
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndMatchingGenericValueAsInnerBeanFactory() throws NoSuchMethodException {
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSample.class)
				.getBeanDefinition();
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
				BeanDefinitionBuilder.rootBeanDefinition(IntegerFactoryBean.class).getBeanDefinition());
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndNonMatchingValue() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(Locale.ENGLISH).getBeanDefinition();
		assertThatIllegalStateException().isThrownBy(() -> resolve(new DefaultListableBeanFactory(), beanDefinition))
				.withMessageContaining(MultiConstructorSample.class.getName())
				.withMessageContaining("and argument types [java.util.Locale]");
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndNonMatchingValueAsInnerBean() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSample.class)
				.addConstructorArgValue(BeanDefinitionBuilder
						.rootBeanDefinition(Locale.class, "getDefault")
						.getBeanDefinition())
				.getBeanDefinition();
		assertThatIllegalStateException().isThrownBy(() -> resolve(new DefaultListableBeanFactory(), beanDefinition))
				.withMessageContaining(MultiConstructorSample.class.getName())
				.withMessageContaining("and argument types [java.util.Locale]");
	}

	@Test
	void detectBeanInstanceExecutableWithFactoryBeanSetInBeanClass() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(
				ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
		beanDefinition.setBeanClass(NumberHolderFactoryBean.class);
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull()
				.isEqualTo(NumberHolderFactoryBean.class.getDeclaredConstructors()[0]);
	}

	@Test
	void detectBeanInstanceExecutableWithFactoryBeanSetInBeanClassAndNoResolvableType() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setBeanClass(NumberHolderFactoryBean.class);
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull()
				.isEqualTo(NumberHolderFactoryBean.class.getDeclaredConstructors()[0]);
	}

	@Test
	void detectBeanInstanceExecutableWithFactoryBeanSetInBeanClassThatDoesNotMatchTargetType() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setTargetType(
				ResolvableType.forClassWithGenerics(NumberHolder.class, String.class));
		beanDefinition.setBeanClass(NumberHolderFactoryBean.class);
		assertThatIllegalStateException()
				.isThrownBy(() -> resolve(beanFactory, beanDefinition))
				.withMessageContaining("Incompatible target type")
				.withMessageContaining(NumberHolder.class.getName())
				.withMessageContaining(NumberHolderFactoryBean.class.getName());
	}

	@Test
	void beanDefinitionWithClassArrayConstructorArgAndStringArrayValueType() throws NoSuchMethodException {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(ConstructorClassArraySample.class.getName())
				.addConstructorArgValue(new String[] { "test1, test2" })
				.getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				ConstructorClassArraySample.class.getDeclaredConstructor(Class[].class));
	}

	@Test
	void beanDefinitionWithClassArrayConstructorArgAndStringValueType() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(ConstructorClassArraySample.class.getName())
				.addConstructorArgValue("test1").getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(
				ConstructorClassArraySample.class.getDeclaredConstructors()[0]);
	}

	@Test
	void beanDefinitionWithClassArrayConstructorArgAndAnotherMatchingConstructor() throws NoSuchMethodException {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorClassArraySample.class.getName())
				.addConstructorArgValue(new String[] { "test1, test2" })
				.getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull()
				.isEqualTo(MultiConstructorClassArraySample.class
						.getDeclaredConstructor(String[].class));
	}

	@Test
	void beanDefinitionWithClassArrayFactoryMethodArgAndStringArrayValueType() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(ClassArrayFactoryMethodSample.class.getName())
				.setFactoryMethod("of")
				.addConstructorArgValue(new String[] { "test1, test2" })
				.getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
				.findMethod(ClassArrayFactoryMethodSample.class, "of", Class[].class));
	}

	@Test
	void beanDefinitionWithClassArrayFactoryMethodArgAndAnotherMatchingConstructor() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(
						ClassArrayFactoryMethodSampleWithAnotherFactoryMethod.class.getName())
				.setFactoryMethod("of").addConstructorArgValue("test1")
				.getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull()
				.isEqualTo(ReflectionUtils.findMethod(
						ClassArrayFactoryMethodSampleWithAnotherFactoryMethod.class, "of",
						String[].class));
	}

	@Test
	void beanDefinitionWithMultiConstructorSimilarArgumentsAndMatchingValues() throws NoSuchMethodException {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSimilarArgumentsSample.class)
				.addConstructorArgValue("Test").addConstructorArgValue(1).addConstructorArgValue(2)
				.getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull()
				.isEqualTo(MultiConstructorSimilarArgumentsSample.class
						.getDeclaredConstructor(String.class, Integer.class, Integer.class));
	}

	@Test
	void beanDefinitionWithMultiConstructorSimilarArgumentsAndNullValueForCommonArgument() throws NoSuchMethodException {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSimilarArgumentsSample.class)
				.addConstructorArgValue(null).addConstructorArgValue(null).addConstructorArgValue("Test")
				.getBeanDefinition();
		Executable executable = resolve(beanFactory, beanDefinition);
		assertThat(executable).isNotNull()
				.isEqualTo(MultiConstructorSimilarArgumentsSample.class
						.getDeclaredConstructor(String.class, Integer.class, String.class));
	}

	@Test
	void beanDefinitionWithMultiConstructorSimilarArgumentsAndNullValueForSpecificArgument() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(MultiConstructorSimilarArgumentsSample.class)
				.addConstructorArgValue(null).addConstructorArgValue(1).addConstructorArgValue(null)
				.getBeanDefinition();
		assertThatIllegalStateException().isThrownBy(() -> resolve(beanFactory, beanDefinition))
				.withMessageContaining(MultiConstructorSimilarArgumentsSample.class.getName());
	}

	@Test
	void beanDefinitionWithMultiArgConstructorAndPrimitiveConversion() throws NoSuchMethodException {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(ConstructorPrimitiveFallback.class)
				.addConstructorArgValue("true").getBeanDefinition();
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isEqualTo(
				ConstructorPrimitiveFallback.class.getDeclaredConstructor(boolean.class));
	}

	@Test
	void beanDefinitionWithFactoryWithOverloadedClassMethodsOnInterface() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder
				.rootBeanDefinition(FactoryWithOverloadedClassMethodsOnInterface.class)
				.setFactoryMethod("byAnnotation").addConstructorArgValue(Nullable.class)
				.getBeanDefinition();
		Executable executable = resolve(new DefaultListableBeanFactory(), beanDefinition);
		assertThat(executable).isEqualTo(ReflectionUtils.findMethod(
				FactoryWithOverloadedClassMethodsOnInterface.class, "byAnnotation",
				Class.class));
	}


	private Executable resolve(DefaultListableBeanFactory beanFactory, BeanDefinition beanDefinition) {
		return new ConstructorResolver(beanFactory).resolveConstructorOrFactoryMethod(
				"testBean", (RootBeanDefinition) beanDefinition);
	}


	static class IntegerFactoryBean implements FactoryBean<Integer> {

		@Override
		public Integer getObject() {
			return 42;
		}

		@Override
		public Class<?> getObjectType() {
			return Integer.class;
		}
	}

	@SuppressWarnings("unused")
	static class MultiConstructorSample {

		MultiConstructorSample(String name) {
		}

		MultiConstructorSample(Integer value) {
		}
	}

	@SuppressWarnings("unused")
	static class MultiConstructorArraySample {

		public MultiConstructorArraySample(String... names) {
		}

		public MultiConstructorArraySample(Integer... values) {
		}
	}

	@SuppressWarnings("unused")
	static class MultiConstructorListSample {

		public MultiConstructorListSample(String name) {
		}

		public MultiConstructorListSample(List<Integer> values) {
		}
	}

	interface DummyInterface {

		static String of(Object o) {
			return o.toString();
		}
	}

	@SuppressWarnings("unused")
	static class DummySampleFactory implements DummyInterface {

		static String of(Integer value) {
			return value.toString();
		}

		protected String resolve(String value) {
			return value;
		}
	}

	@SuppressWarnings("unused")
	static class ExtendedSampleFactory extends DummySampleFactory {

		@Override
		protected String resolve(String value) {
			return super.resolve(value);
		}
	}

	@SuppressWarnings("unused")
	static class ConstructorClassArraySample {

		ConstructorClassArraySample(Class<?>... classArrayArg) {
		}

		ConstructorClassArraySample(Executor somethingElse) {
		}
	}

	@SuppressWarnings("unused")
	static class MultiConstructorClassArraySample {

		MultiConstructorClassArraySample(Class<?>... classArrayArg) {
		}

		MultiConstructorClassArraySample(String... stringArrayArg) {
		}
	}

	static class MultiConstructorSimilarArgumentsSample {

		MultiConstructorSimilarArgumentsSample(String name, Integer counter, String value) {
		}

		MultiConstructorSimilarArgumentsSample(String name, Integer counter, Integer value) {
		}
	}

	@SuppressWarnings("unused")
	static class ClassArrayFactoryMethodSample {

		static String of(Class<?>[] classArrayArg) {
			return "test";
		}
	}

	@SuppressWarnings("unused")
	static class ClassArrayFactoryMethodSampleWithAnotherFactoryMethod {

		static String of(Class<?>[] classArrayArg) {
			return "test";
		}

		static String of(String[] classArrayArg) {
			return "test";
		}
	}

	@SuppressWarnings("unnused")
	static class ConstructorPrimitiveFallback {

		public ConstructorPrimitiveFallback(boolean useDefaultExecutor) {
		}

		public ConstructorPrimitiveFallback(Executor executor) {
		}
	}

	static class SampleBeanWithConstructors {

		public SampleBeanWithConstructors() {
		}

		public SampleBeanWithConstructors(String name) {
		}

		public SampleBeanWithConstructors(Number number, String name) {
		}
	}

	interface FactoryWithOverloadedClassMethodsOnInterface {

		static FactoryWithOverloadedClassMethodsOnInterface byAnnotation(
				Class<? extends Annotation> annotationType) {
			return byAnnotation(annotationType, SearchStrategy.INHERITED_ANNOTATIONS);
		}

		static FactoryWithOverloadedClassMethodsOnInterface byAnnotation(
				Class<? extends Annotation> annotationType,
				SearchStrategy searchStrategy) {
			return null;
		}
	}

}
