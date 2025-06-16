/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * {@link AnnotationMetadata} implementation that uses standard reflection
 * to introspect a given {@link Class}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.5
 */
public class StandardAnnotationMetadata extends StandardClassMetadata implements AnnotationMetadata {

	private final MergedAnnotations mergedAnnotations;

	private final boolean nestedAnnotationsAsMap;

	private @Nullable Set<String> annotationTypes;


	/**
	 * Create a new {@code StandardAnnotationMetadata} wrapper for the given Class.
	 * @param introspectedClass the Class to introspect
	 * @see #StandardAnnotationMetadata(Class, boolean)
	 * @deprecated in favor of the factory method {@link AnnotationMetadata#introspect(Class)}
	 */
	@Deprecated(since = "5.2")
	public StandardAnnotationMetadata(Class<?> introspectedClass) {
		this(introspectedClass, false);
	}

	/**
	 * Create a new {@link StandardAnnotationMetadata} wrapper for the given Class,
	 * providing the option to return any nested annotations or annotation arrays in the
	 * form of {@link org.springframework.core.annotation.AnnotationAttributes} instead
	 * of actual {@link Annotation} instances.
	 * @param introspectedClass the Class to introspect
	 * @param nestedAnnotationsAsMap return nested annotations and annotation arrays as
	 * {@link org.springframework.core.annotation.AnnotationAttributes} for compatibility
	 * with ASM-based {@link AnnotationMetadata} implementations
	 * @since 3.1.1
	 * @deprecated in favor of the factory method {@link AnnotationMetadata#introspect(Class)}.
	 * Use {@link MergedAnnotation#asMap(org.springframework.core.annotation.MergedAnnotation.Adapt...) MergedAnnotation.asMap}
	 * from {@link #getAnnotations()} rather than {@link #getAnnotationAttributes(String)}
	 * if {@code nestedAnnotationsAsMap} is {@code false}
	 */
	@Deprecated(since = "5.2")
	public StandardAnnotationMetadata(Class<?> introspectedClass, boolean nestedAnnotationsAsMap) {
		super(introspectedClass);
		this.mergedAnnotations = MergedAnnotations.from(introspectedClass,
				SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none());
		this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
	}


	@Override
	public MergedAnnotations getAnnotations() {
		return this.mergedAnnotations;
	}

	@Override
	public Set<String> getAnnotationTypes() {
		Set<String> annotationTypes = this.annotationTypes;
		if (annotationTypes == null) {
			annotationTypes = Collections.unmodifiableSet(AnnotationMetadata.super.getAnnotationTypes());
			this.annotationTypes = annotationTypes;
		}
		return annotationTypes;
	}

	@Override
	public @Nullable Map<String, @Nullable Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
		if (this.nestedAnnotationsAsMap) {
			return AnnotationMetadata.super.getAnnotationAttributes(annotationName, classValuesAsString);
		}
		return AnnotatedElementUtils.getMergedAnnotationAttributes(
				getIntrospectedClass(), annotationName, classValuesAsString, false);
	}

	@Override
	@SuppressWarnings("NullAway") // Null-safety of Java super method not yet managed
	public @Nullable MultiValueMap<String, @Nullable Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
		if (this.nestedAnnotationsAsMap) {
			return AnnotationMetadata.super.getAllAnnotationAttributes(annotationName, classValuesAsString);
		}
		return AnnotatedElementUtils.getAllAnnotationAttributes(
				getIntrospectedClass(), annotationName, classValuesAsString, false);
	}

	@Override
	public boolean hasAnnotatedMethods(String annotationName) {
		if (AnnotationUtils.isCandidateClass(getIntrospectedClass(), annotationName)) {
			try {
				Method[] methods = ReflectionUtils.getDeclaredMethods(getIntrospectedClass());
				for (Method method : methods) {
					if (isAnnotatedMethod(method, annotationName)) {
						return true;
					}
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to introspect annotated methods on " + getIntrospectedClass(), ex);
			}
		}
		return false;
	}

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		Set<MethodMetadata> result = new LinkedHashSet<>(4);
		if (AnnotationUtils.isCandidateClass(getIntrospectedClass(), annotationName)) {
			ReflectionUtils.doWithLocalMethods(getIntrospectedClass(), method -> {
				if (isAnnotatedMethod(method, annotationName)) {
					result.add(new StandardMethodMetadata(method, this.nestedAnnotationsAsMap));
				}
			});
		}
		return result;
	}

	@Override
	public Set<MethodMetadata> getDeclaredMethods() {
		Set<MethodMetadata> result = new LinkedHashSet<>(16);
		ReflectionUtils.doWithLocalMethods(getIntrospectedClass(), method ->
				result.add(new StandardMethodMetadata(method, this.nestedAnnotationsAsMap)));
		return result;
	}


	private static boolean isAnnotatedMethod(Method method, String annotationName) {
		return !method.isBridge() && method.getAnnotations().length > 0 &&
				AnnotatedElementUtils.isAnnotated(method, annotationName);
	}

	static AnnotationMetadata from(Class<?> introspectedClass) {
		return new StandardAnnotationMetadata(introspectedClass, true);
	}

}
