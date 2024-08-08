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

package org.springframework.test.context.bean.override.convention;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.TestBeanFactory;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link TestBeanOverrideProcessor}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
class TestBeanOverrideProcessorTests {

	private final TestBeanOverrideProcessor processor = new TestBeanOverrideProcessor();

	@Test
	void findTestBeanFactoryMethodFindsFromCandidateNames() {
		Class<?> clazz = MethodConventionTestCase.class;
		Class<?> returnType = ExampleService.class;

		Method method = this.processor.findTestBeanFactoryMethod(
				clazz, returnType, "example1", "example2", "example3");

		assertThat(method.getName()).isEqualTo("example2");
	}

	@Test
	void findTestBeanFactoryMethodFindsLocalMethodWhenSubclassMethodHidesSuperclassMethod() {
		Class<?> clazz = SubTestCase.class;
		Class<?> returnType = String.class;

		Method method = this.processor.findTestBeanFactoryMethod(clazz, returnType, "factory");

		assertThat(method).isEqualTo(ReflectionUtils.findMethod(clazz, "factory"));
	}

	@Test
	void findTestBeanFactoryMethodNotFound() {
		Class<?> clazz = MethodConventionTestCase.class;
		Class<?> returnType = ExampleService.class;

		assertThatIllegalStateException()
				.isThrownBy(() -> this.processor.findTestBeanFactoryMethod(clazz, returnType, "example1", "example3"))
				.withMessage("No static method found named example1() or example3() in %s with return type %s",
						MethodConventionTestCase.class.getName(), ExampleService.class.getName());
	}

	@Test
	void findTestBeanFactoryMethodTwoFound() {
		Class<?> clazz = MethodConventionTestCase.class;
		Class<?> returnType = ExampleService.class;

		assertThatIllegalStateException()
				.isThrownBy(() -> this.processor.findTestBeanFactoryMethod(clazz, returnType, "example2", "example4"))
				.withMessage("Found 2 competing static methods named example2() or example4() in %s with return type %s",
						clazz.getName(), returnType.getName());
	}

	@Test
	void findTestBeanFactoryMethodNoNameProvided() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.processor.findTestBeanFactoryMethod(MethodConventionTestCase.class, ExampleService.class))
				.withMessage("At least one candidate method name is required");
	}

	@Test
	void findTestBeanFactoryMethodByFullyQualifiedName() {
		Class<?> clazz = getClass();
		Class<?> returnType = String.class;
		String methodName = TestBeanFactory.class.getName() + "#createTestMessage";

		Method method = this.processor.findTestBeanFactoryMethod(clazz, returnType, methodName);

		assertThat(method).isEqualTo(ReflectionUtils.findMethod(TestBeanFactory.class, "createTestMessage"));
	}

	@Test
	void findTestBeanFactoryMethodByFullyQualifiedNameWithNonexistentMethod() {
		Class<?> clazz = getClass();
		Class<?> returnType = String.class;
		String factoryClassName = TestBeanFactory.class.getName();
		String methodName = factoryClassName + "#bogus";

		assertThatIllegalStateException()
				.isThrownBy(() -> this.processor.findTestBeanFactoryMethod(clazz, returnType, methodName))
				.withMessage("No static method found named %s in %s with return type %s",
						"bogus", factoryClassName, returnType.getName());
	}

	@Test
	void findTestBeanFactoryMethodByFullyQualifiedNameWithNonexistentClass() {
		Class<?> clazz = getClass();
		Class<?> returnType = String.class;
		String methodName = "org.example.Bogus#createTestBean";

		assertThatIllegalStateException()
				.isThrownBy(() -> this.processor.findTestBeanFactoryMethod(clazz, returnType, methodName))
				.withMessage("Failed to load class for fully-qualified method name: %s", methodName)
				.withCauseInstanceOf(ClassNotFoundException.class);
	}

	@Test
	void findTestBeanFactoryMethodByFullyQualifiedNameWithMissingMethodName() {
		Class<?> clazz = getClass();
		Class<?> returnType = String.class;
		String methodName = TestBeanFactory.class.getName() + "#";

		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.processor.findTestBeanFactoryMethod(clazz, returnType, methodName))
				.withMessage("No method name present in fully-qualified method name: %s", methodName);
	}

	@Test
	void findTestBeanFactoryMethodByFullyQualifiedNameWithMissingClassName() {
		Class<?> clazz = getClass();
		Class<?> returnType = String.class;
		String methodName = "#createTestBean";

		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.processor.findTestBeanFactoryMethod(clazz, returnType, methodName))
				.withMessage("No class name present in fully-qualified method name: %s", methodName);
	}

	@Test
	void createMetaDataForUnknownExplicitMethod() throws Exception {
		Class<?> clazz = ExplicitMethodNameTestCase.class;
		Class<?> returnType = ExampleService.class;
		Field field = clazz.getField("a");
		TestBean overrideAnnotation = field.getAnnotation(TestBean.class);
		assertThat(overrideAnnotation).isNotNull();

		assertThatIllegalStateException()
				.isThrownBy(() -> this.processor.createMetadata(overrideAnnotation, clazz, field))
				.withMessage("No static method found named explicit1() in %s with return type %s",
						clazz.getName(), returnType.getName());
	}

	@Test
	void createMetaDataForKnownExplicitMethod() throws Exception {
		Class<?> clazz = ExplicitMethodNameTestCase.class;
		Field field = clazz.getField("b");
		TestBean overrideAnnotation = field.getAnnotation(TestBean.class);
		assertThat(overrideAnnotation).isNotNull();

		assertThat(this.processor.createMetadata(overrideAnnotation, clazz, field))
				.isInstanceOf(TestBeanOverrideMetadata.class);
	}

	@Test
	void createMetaDataForConventionBasedFactoryMethod() throws Exception {
		Class<?> returnType = ExampleService.class;
		Class<?> clazz = MethodConventionTestCase.class;
		Field field = clazz.getField("field");
		TestBean overrideAnnotation = field.getAnnotation(TestBean.class);
		assertThat(overrideAnnotation).isNotNull();

		assertThatIllegalStateException()
				.isThrownBy(() -> this.processor.createMetadata(overrideAnnotation, clazz, field))
				.withMessage("No static method found named field() or someField() in %s with return type %s",
						clazz.getName(), returnType.getName());
	}

	@Test
	void failToCreateMetadataForOtherAnnotation() throws NoSuchFieldException {
		Class<?> clazz = MethodConventionTestCase.class;
		Field field = clazz.getField("field");
		NonNull badAnnotation = AnnotationUtils.synthesizeAnnotation(NonNull.class);

		assertThatIllegalStateException()
				.isThrownBy(() -> this.processor.createMetadata(badAnnotation, clazz, field))
				.withMessage("Invalid annotation passed to TestBeanOverrideProcessor: expected @TestBean" +
								" on field %s.%s", field.getDeclaringClass().getName(), field.getName());
	}


	static class MethodConventionTestCase {

		@TestBean(name = "someField")
		public ExampleService field;

		ExampleService example1() {
			return null;
		}

		static ExampleService example2() {
			return null;
		}

		static ExampleService example4() {
			return null;
		}
	}

	static class ExplicitMethodNameTestCase {

		@TestBean(methodName = "explicit1")
		public ExampleService a;

		@TestBean(methodName = "explicit2")
		public ExampleService b;

		static ExampleService explicit2() {
			return null;
		}
	}

	static class BaseTestCase {

		public String field;

		static String factory() {
			return null;
		}
	}

	static class SubTestCase extends BaseTestCase {

		// Hides factory() in superclass.
		static String factory() {
			return null;
		}
	}

}
