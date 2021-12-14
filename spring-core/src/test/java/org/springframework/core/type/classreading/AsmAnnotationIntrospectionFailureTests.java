/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.type.classreading;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Test;

import org.springframework.core.OverridingClassLoader;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

/**
 * Tests that trigger annotation introspection failures when using Spring's
 * ASM-based annotation processing and ensure that they are dealt with correctly.
 *
 * <p>This test class also contains tests that verify the behavior when using
 * the JDK's standard reflection APIs for comparison with Spring's ASM-based
 * annotation processing.
 * 
 * @author Sam Brannen
 * @since 5.1.21
 */
public class AsmAnnotationIntrospectionFailureTests {

	private final ClassLoader standardClassLoader = getClass().getClassLoader();

	private final FilteringClassLoader filteringClassLoader = new FilteringClassLoader(standardClassLoader);


	@Test
	public void jdkLoadableAnnotationsAndClassAttributes() throws Exception {
		Annotation filteredAnnotation = retrieveAnnotationViaReflection(AnnotationWithClassAttributeClass.class.getName(),
				FilteredAnnotation.class.getName(), standardClassLoader);
		assertNotNull("@FilteredAnnotation", filteredAnnotation);
		assertEquals(FilteredAnnotation.class, filteredAnnotation.annotationType());

		Object text = getTextInAnnotationWithClassAttribute(standardClassLoader);
		assertEquals("enigma", text);

		Object clazz = getClazzInAnnotationWithClassAttribute(standardClassLoader);
		assertEquals(FilteredType.class, clazz);
	}

	@Test
	public void jdkNonLoadableAnnotationsAndClassAttributes() throws Exception {
		Annotation filteredAnnotation = retrieveAnnotationViaReflection(AnnotationWithClassAttributeClass.class.getName(),
				FilteredAnnotation.class.getName(), filteringClassLoader);
		assertNull("JDK ignores annotations whose types cannot be loaded", filteredAnnotation);

		Object text = getTextInAnnotationWithClassAttribute(filteringClassLoader);
		assertEquals("JDK allows access to attributes unaffected by non-loadable types", "enigma", text);

		// JDK throws TypeNotPresentException when accessing attributes with non-loadable types
		Exception exception = assertThrows(TypeNotPresentException.class, () -> getClazzInAnnotationWithClassAttribute(filteringClassLoader));
		assertCauseInstanceOfClassNotFoundException(exception);
	}

	@Test
	public void springAsmNonLoadableAnnotationsAndClassAttributesWithDirectlyPresentAnnotations() throws Exception {
		AnnotationMetadata annotationMetadata = annotationMetadata(AnnotationWithClassAttributeClass.class.getName(), filteringClassLoader);
		assertNotNull("Spring scans all annotation metadata even if some types cannot be loaded", annotationMetadata);

		Object filteredAnnotation = annotationMetadata.getAnnotationAttributes(FilteredAnnotation.class.getName());
		assertNull("Spring ignores annotations whose types cannot be loaded", filteredAnnotation);

		AnnotationAttributes annotationWithClassAttribute =
				(AnnotationAttributes) annotationMetadata.getAnnotationAttributes(AnnotationWithClassAttribute.class.getName());
		assertNotNull("@AnnotationWithClassAttribute", annotationWithClassAttribute);
		assertEquals("Spring allows access to attributes unaffected by non-loadable types",
				"enigma", annotationWithClassAttribute.getString("text"));

		// Spring throws an IllegalArgumentException when accessing attributes with non-loadable types
		Exception exception = assertThrows(IllegalArgumentException.class, () -> annotationWithClassAttribute.getClass("clazz"));
		assertCauseInstanceOfClassNotFoundException(exception);
	}

	@Test
	public void springAsmNonLoadableAnnotationsAndClassAttributesWithMergedComposedAnnotations() throws Exception {
		AnnotationMetadata annotationMetadata = annotationMetadata(ComposedAnnotationClass.class.getName(), filteringClassLoader);
		assertNotNull("Spring scans all annotation metadata even if some types cannot be loaded", annotationMetadata);

		Object filteredAnnotation = annotationMetadata.getAnnotationAttributes(FilteredAnnotation.class.getName());
		assertNull("Spring ignores annotations whose types cannot be loaded", filteredAnnotation);

		AnnotationAttributes composedAnnotation =
				(AnnotationAttributes) annotationMetadata.getAnnotationAttributes(ComposedAnnotation.class.getName());
		assertNotNull("@ComposedAnnotation", composedAnnotation);

		assertEquals("Spring allows access to attributes unaffected by non-loadable types",
				"enigma", composedAnnotation.getString("text"));

		// Spring throws an IllegalArgumentException when accessing attributes with non-loadable types
		Exception exception = assertThrows(IllegalArgumentException.class, () -> composedAnnotation.getClass("example1"));
		assertCauseInstanceOfClassNotFoundException(exception);
		exception = assertThrows(IllegalArgumentException.class, () -> composedAnnotation.getClass("example2"));
		assertCauseInstanceOfClassNotFoundException(exception);
	}

	@Test
	public void jdkLoadableNestedAnnotationAttributes() throws Exception {
		Annotation nestedAnnotationContainer = retrieveAnnotationViaReflection(NestedAnnotationContainerClass.class.getName(),
				NestedAnnotationContainer.class.getName(), standardClassLoader);
		assertNotNull("@NestedAnnotationContainer", nestedAnnotationContainer);
		assertEquals(NestedAnnotationContainer.class, nestedAnnotationContainer.annotationType());

		Object text = getTextInNestedAnnotationContainer(standardClassLoader);
		assertEquals("enigma", text);

		FilteredNestedAnnotation nested = (FilteredNestedAnnotation) getNestedInNestedAnnotationContainer(standardClassLoader);
		assertNotNull("@FilteredNestedAnnotation", nested);
		assertEquals("nested!", nested.value());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void jdkNonLoadableNestedAnnotationAttributes() throws Exception {
		Class<?> nestedAnnotationContainerClass = Class.forName(NestedAnnotationContainerClass.class.getName(), false, filteringClassLoader);
		assertNotNull("A class annotated with @NestedAnnotationContainer", nestedAnnotationContainerClass);

		Class<? extends Annotation> nestedAnnotationContainerType =
				(Class<? extends Annotation>) Class.forName(NestedAnnotationContainer.class.getName(), false, filteringClassLoader);
		assertNotNull("The @NestedAnnotationContainer class itself", nestedAnnotationContainerType);

		// JDK throws NoClassDefFoundError when accessing any annotations on a class, if any
		// annotation declared on the class has nested annotation attributes with non-loadable types.
		// This is because the JDK eagerly instantiates all annotation types present on
		// an annotated element even if certain annotation instances will never be used.
		Error error = assertThrows(NoClassDefFoundError.class, () -> nestedAnnotationContainerClass.getAnnotation(nestedAnnotationContainerType));
		assertCauseInstanceOfClassNotFoundException(error);
	}

	@Test
	public void springAsmNonLoadableNestedAnnotationAttributes() throws Exception {
		AnnotationMetadata annotationMetadata = annotationMetadata(NestedAnnotationContainerClass.class.getName(), filteringClassLoader);
		assertNotNull("Spring scans all annotation metadata even if some types cannot be loaded", annotationMetadata);

		// In contrast to the JDK, Spring actually allows access to annotation metadata
		// on a class even if another annotation declared on the class has nested annotation
		// attributes with non-loadable types.
		//
		// In this scenario we can access @AnnotationWithClassAttribute metadata, but we cannot
		// access @NestedAnnotationContainer metadata (see assertThrows() below).
		AnnotationAttributes annotationWithClassAttribute =
				(AnnotationAttributes) annotationMetadata.getAnnotationAttributes(AnnotationWithClassAttribute.class.getName());
		assertNotNull("@AnnotationWithClassAttribute", annotationWithClassAttribute);
		assertEquals("enigma", annotationWithClassAttribute.getString("text"));

		// Similar to the JDK, Spring throws NoClassDefFoundError when accessing annotation attributes
		// if the annotation has nested annotation attributes with non-loadable types. This is because
		// getAnnotationAttributes() delegates to AnnotationReadingVisitorUtils.convertClassValues()
		// which indirectly delegates to AnnotationUtils.getAttributeMethods() which delegates to
		// Class.getDeclaredMethods() (where the class is the annotation type), and this is
		// equivalent to what the JDK does when preparing to instantiate all annotations present on the class.
		Error error = assertThrows(NoClassDefFoundError.class, () -> annotationMetadata.getAnnotationAttributes(NestedAnnotationContainer.class.getName()));
		assertCauseInstanceOfClassNotFoundException(error);
	}

	@Test
	public void jdkLoadableEnumAttributes() throws Exception {
		Annotation annotationWithEnumAttribute = retrieveAnnotationViaReflection(AnnotationWithEnumAttributeClass.class.getName(),
				AnnotationWithEnumAttribute.class.getName(), standardClassLoader);
		assertNotNull("@AnnotationWithEnumAttribute", annotationWithEnumAttribute);
		assertEquals(AnnotationWithEnumAttribute.class, annotationWithEnumAttribute.annotationType());

		Object color = getColorInAnnotationWithEnumAttribute(standardClassLoader);
		assertEquals(Color.GREEN, color);

		Object filteredEnum = getFilteredEnumInAnnotationWithEnumAttribute(standardClassLoader);
		assertEquals(FilteredEnum.FOO, filteredEnum);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void jdkNonLoadableEnumAttributes() throws Exception {
		Class<?> annotationWithEnumAttributeClass = Class.forName(AnnotationWithEnumAttributeClass.class.getName(), false, filteringClassLoader);
		assertNotNull("A class annotated with @AnnotationWithEnumAttribute", annotationWithEnumAttributeClass);

		Class<? extends Annotation> annotationWithEnumAttributeType =
				(Class<? extends Annotation>) Class.forName(AnnotationWithEnumAttribute.class.getName(), false, filteringClassLoader);
		assertNotNull("The @AnnotationWithEnumAttribute class itself", annotationWithEnumAttributeType);

		// JDK throws NoClassDefFoundError when accessing any annotations on a class, if any
		// annotation declared on the class has enum attributes with non-loadable types.
		// This is because the JDK eagerly instantiates all annotation types present on
		// an annotated element even if certain annotation instances will never be used.
		Error error = assertThrows(NoClassDefFoundError.class, () -> annotationWithEnumAttributeClass.getAnnotation(annotationWithEnumAttributeType));
		assertCauseInstanceOfClassNotFoundException(error);
	}

	@Test
	public void springAsmNonLoadableEnumAttributes() throws Exception {
		AnnotationMetadata annotationMetadata = annotationMetadata(AnnotationWithEnumAttributeClass.class.getName(), filteringClassLoader);
		assertNotNull("Spring scans all annotation metadata even if some types cannot be loaded", annotationMetadata);

		// In contrast to the JDK, Spring actually allows access to annotation metadata
		// on a class even if another annotation declared on the class has enum attributes
		// with non-loadable types.
		//
		// In this scenario we can access @AnnotationWithClassAttribute metadata, but we cannot
		// access @AnnotationWithEnumAttribute metadata (see assertThrows() below).
		AnnotationAttributes annotationWithClassAttribute =
				(AnnotationAttributes) annotationMetadata.getAnnotationAttributes(AnnotationWithClassAttribute.class.getName());
		assertNotNull("@AnnotationWithClassAttribute", annotationWithClassAttribute);
		assertEquals("enigma", annotationWithClassAttribute.getString("text"));

		// Similar to the JDK, Spring throws NoClassDefFoundError when accessing annotation
		// attributes if the annotation has enum attributes with non-loadable types. This is because
		// getAnnotationAttributes() delegates to AnnotationReadingVisitorUtils.convertClassValues()
		// which indirectly delegates to AnnotationUtils.getAttributeMethods() which delegates to
		// Class.getDeclaredMethods() (where the class is the annotation type), and this is
		// equivalent to what the JDK does when preparing to instantiate all annotations present on the class.
		Error error = assertThrows(NoClassDefFoundError.class, () -> annotationMetadata.getAnnotationAttributes(AnnotationWithEnumAttribute.class.getName()));
		assertCauseInstanceOfClassNotFoundException(error);
	}

	private static Object getTextInAnnotationWithClassAttribute(ClassLoader classLoader) throws Exception {
		Annotation annotation = retrieveAnnotationViaReflection(AnnotationWithClassAttributeClass.class.getName(),
				AnnotationWithClassAttribute.class.getName(), classLoader);
		return invokeAttributeMethod(annotation, "text");
	}

	private static Object getClazzInAnnotationWithClassAttribute(ClassLoader classLoader) throws Exception {
		Annotation annotation = retrieveAnnotationViaReflection(AnnotationWithClassAttributeClass.class.getName(),
				AnnotationWithClassAttribute.class.getName(), classLoader);
		return invokeAttributeMethod(annotation, "clazz");
	}

	private static Object getTextInNestedAnnotationContainer(ClassLoader classLoader) throws Exception {
		Annotation annotation = retrieveAnnotationViaReflection(NestedAnnotationContainerClass.class.getName(),
				NestedAnnotationContainer.class.getName(), classLoader);
		return invokeAttributeMethod(annotation, "text");
	}

	private static Object getNestedInNestedAnnotationContainer(ClassLoader classLoader) throws Exception {
		Annotation annotation = retrieveAnnotationViaReflection(NestedAnnotationContainerClass.class.getName(),
				NestedAnnotationContainer.class.getName(), classLoader);
		return invokeAttributeMethod(annotation, "nested");
	}

	private static Object getColorInAnnotationWithEnumAttribute(ClassLoader classLoader) throws Exception {
		Annotation annotation = retrieveAnnotationViaReflection(AnnotationWithEnumAttributeClass.class.getName(),
				AnnotationWithEnumAttribute.class.getName(), classLoader);
		return invokeAttributeMethod(annotation, "color");
	}

	private static Object getFilteredEnumInAnnotationWithEnumAttribute(ClassLoader classLoader) throws Exception {
		Annotation annotation = retrieveAnnotationViaReflection(AnnotationWithEnumAttributeClass.class.getName(),
				AnnotationWithEnumAttribute.class.getName(), classLoader);
		return invokeAttributeMethod(annotation, "filteredEnum");
	}

	private static Annotation retrieveAnnotationViaReflection(String annotatedClassName, String annotationName, ClassLoader classLoader) throws Exception {
		Class<?> annotatedClass = Class.forName(annotatedClassName, false, classLoader);
		return Arrays.stream(annotatedClass.getAnnotations())
				.filter(annotation -> annotation.annotationType().getName().equals(annotationName))
				.findFirst()
				.orElse(null);
	}

	private static Object invokeAttributeMethod(Annotation annotation, String attributeName) throws Exception {
		Method method = annotation.annotationType().getMethod(attributeName);
		method.setAccessible(true);
		return ReflectionUtils.invokeMethod(method, annotation);
	}

	private static AnnotationMetadata annotationMetadata(String className, ClassLoader classLoader) {
		try {
			return new SimpleMetadataReaderFactory(classLoader).getMetadataReader(className).getAnnotationMetadata();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T assertThrows(Class<T> exceptionType, Executable executable) {
		try {
			executable.execute();
		}
		catch (Throwable ex) {
			if (exceptionType.isInstance(ex)) {
				return (T) ex;
			} else {
				fail(String.format("Threw <%s> but should have thrown <%s>", ex.getClass().getName(), exceptionType.getName()));
			}
		}
		throw new AssertionError(String.format("Should have thrown <%s>", exceptionType.getName()));
	}

	private static void assertCauseInstanceOfClassNotFoundException(Throwable ex) {
		if (!ClassNotFoundException.class.isInstance(ex.getCause())) {
			fail(String.format("<%s> should be an instance of <%s>", ex.getClass().getName(), ClassNotFoundException.class.getName()));
		}
	}


	static class FilteredType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface FilteredAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AnnotationWithClassAttribute {

		String text() default "";

		Class<?> clazz() default Void.class;

	}

	@FilteredAnnotation
	@AnnotationWithClassAttribute(clazz = FilteredType.class, text = "enigma")
	static class AnnotationWithClassAttributeClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface FilteredNestedAnnotation {

		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NestedAnnotationContainer {
		
		String text();

		FilteredNestedAnnotation nested();

	}

	@FilteredAnnotation
	@AnnotationWithClassAttribute(clazz = String.class, text = "enigma")
	@NestedAnnotationContainer(text = "enigma", nested = @FilteredNestedAnnotation("nested!"))
	static class NestedAnnotationContainerClass {
	}

	enum Color {
		GREEN, BLUE;
	}

	enum FilteredEnum {
		FOO, BAR;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AnnotationWithEnumAttribute {

		Color color();

		FilteredEnum filteredEnum() default FilteredEnum.BAR;

	}

	@FilteredAnnotation
	@AnnotationWithClassAttribute(clazz = String.class, text = "enigma")
	@AnnotationWithEnumAttribute(color = Color.GREEN, filteredEnum = FilteredEnum.FOO)
	static class AnnotationWithEnumAttributeClass {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@AnnotationWithClassAttribute
	@interface ComposedAnnotation {

		@AliasFor(annotation = AnnotationWithClassAttribute.class)
		String text() default "";

		@AliasFor(annotation = AnnotationWithClassAttribute.class, attribute = "clazz")
		Class<?> example1() default Void.class;

		@AliasFor(annotation = AnnotationWithClassAttribute.class, attribute = "clazz")
		Class<?> example2() default Void.class;

	}

	@FilteredAnnotation
	@ComposedAnnotation(example1 = FilteredType.class, text = "enigma")
	static class ComposedAnnotationClass {
	}

	private static class FilteringClassLoader extends OverridingClassLoader {

		FilteringClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			return className.startsWith(AsmAnnotationIntrospectionFailureTests.class.getName());
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (isEligibleForOverriding(name) && name.contains("Filtered")) {
				throw new ClassNotFoundException(name);
			}
			return super.loadClass(name, resolve);
		}
	}

	@FunctionalInterface
	private static interface Executable {
		void execute() throws Exception;
	}

}
