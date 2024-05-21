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

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.AnnotationSpec;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Helper class to register warnings that the compiler may trigger on
 * generated code.
 *
 * @author Stephane Nicoll
 * @since 6.1
 * @see SuppressWarnings
 */
class CodeWarnings {

	private final Set<String> warnings = new LinkedHashSet<>();


	/**
	 * Register a warning to be included for this block. Does nothing if
	 * the warning is already registered.
	 * @param warning the warning to register, if it hasn't been already
	 */
	public void register(String warning) {
		this.warnings.add(warning);
	}

	/**
	 * Detect the presence of {@link Deprecated} on the specified elements.
	 * @param elements the elements to check
	 * @return {@code this} instance
	 */
	public CodeWarnings detectDeprecation(AnnotatedElement... elements) {
		for (AnnotatedElement element : elements) {
			register(element.getAnnotation(Deprecated.class));
		}
		return this;
	}

	/**
	 * Detect the presence of {@link Deprecated} on the specified elements.
	 * @param elements the elements to check
	 * @return {@code this} instance
	 */
	public CodeWarnings detectDeprecation(Stream<AnnotatedElement> elements) {
		elements.forEach(element -> register(element.getAnnotation(Deprecated.class)));
		return this;
	}

	/**
	 * Detect the presence of {@link Deprecated} on the signature of the
	 * specified {@link ResolvableType}.
	 * @param resolvableType a type signature
	 * @return {@code this} instance
	 */
	public CodeWarnings detectDeprecation(ResolvableType resolvableType) {
		if (ResolvableType.NONE.equals(resolvableType)) {
			return this;
		}
		Class<?> type = ClassUtils.getUserClass(resolvableType.toClass());
		detectDeprecation(type);
		if (resolvableType.hasGenerics() && !resolvableType.hasUnresolvableGenerics()) {
			for (ResolvableType generic : resolvableType.getGenerics()) {
				detectDeprecation(generic);
			}
		}
		return this;
	}

	/**
	 * Include {@link SuppressWarnings} on the specified method if necessary.
	 * @param method the method to update
	 */
	public void suppress(MethodSpec.Builder method) {
		if (this.warnings.isEmpty()) {
			return;
		}
		method.addAnnotation(buildAnnotationSpec());
	}

	/**
	 * Return the currently registered warnings.
	 * @return the warnings
	 */
	protected Set<String> getWarnings() {
		return Collections.unmodifiableSet(this.warnings);
	}

	private void register(@Nullable Deprecated annotation) {
		if (annotation != null) {
			if (annotation.forRemoval()) {
				register("removal");
			}
			else {
				register("deprecation");
			}
		}
	}

	private AnnotationSpec buildAnnotationSpec() {
		return AnnotationSpec.builder(SuppressWarnings.class)
				.addMember("value", generateValueCode()).build();
	}

	private CodeBlock generateValueCode() {
		if (this.warnings.size() == 1) {
			return CodeBlock.of("$S", this.warnings.iterator().next());
		}
		CodeBlock values = CodeBlock.join(this.warnings.stream()
				.map(warning -> CodeBlock.of("$S", warning)).toList(), ", ");
		return CodeBlock.of("{ $L }", values);
	}

	@Override
	public String toString() {
		return CodeWarnings.class.getSimpleName() + this.warnings;
	}

}
